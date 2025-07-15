/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.audio_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.audio_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.services.CallService
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.notificationHelper
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientIncomingCallListener
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.AudioFileUsage
import com.voximplant.sdk.hardware.IAudioDeviceEventsListener
import com.voximplant.sdk.hardware.IAudioDeviceManager
import com.voximplant.sdk.hardware.IAudioFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

abstract class AudioCallManager(
    private val context: Context,
    private val client: IClient,
) : IClientIncomingCallListener, ICallListener, IEndpointListener, IAudioDeviceEventsListener {

    // Call management
    protected var managedCall: ICall? = null
    val callExists: Boolean
        get() = managedCall != null
    private val _callState = MutableStateFlow(CallState.NONE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    private val _previousCallState = MutableLiveData(CallState.NONE)
    private var _callTimer: Timer = Timer("callTimer")
    private val _callDuration = MutableLiveData(0L)
    val callDuration: LiveData<Long>
        get() = _callDuration
    private val callBroadcastReceiver: BroadcastReceiver = AudioCallBroadcastReceiver()
    private val callSettings: CallSettings
        get() = CallSettings().apply { videoFlags = VideoFlags(false, false) }

    // Call properties
    var endpointDisplayName: String? = null
        protected set
    var endpointUsername: String? = null
        protected set
    private val _muted = MutableLiveData(false)
    val muted: LiveData<Boolean>
        get() = _muted
    private val _onHold = MutableLiveData(false)
    val onHold: LiveData<Boolean>
        get() = _onHold

    protected val audioDeviceManager: IAudioDeviceManager = Voximplant.getAudioDeviceManager()

    private val _selectedAudioDevice = MutableStateFlow(audioDeviceManager.activeDevice)
    val selectedAudioDevice: StateFlow<AudioDevice> = _selectedAudioDevice.asStateFlow()
    private val _availableAudioDevices = MutableStateFlow(audioDeviceManager.audioDevices)
    val availableAudioDevices = _availableAudioDevices.asStateFlow()

    private var callProgressToneFile: IAudioFile? = Voximplant.createAudioFile(context, R.raw.call_progress_tone, AudioFileUsage.IN_CALL)
    private var callReconnectingToneFile: IAudioFile? = Voximplant.createAudioFile(context, R.raw.call_reconnecting_tone, AudioFileUsage.IN_CALL)
    private var callConnectedToneFile: IAudioFile? = Voximplant.createAudioFile(context, R.raw.call_connected_tone, AudioFileUsage.IN_CALL)
    private var callFailedToneFile: IAudioFile? = Voximplant.createAudioFile(context, R.raw.call_failed_tone, AudioFileUsage.IN_CALL)

    // Call events
    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null
    var onCallAnswer: (() -> Unit)? = null

    init {
        client.setClientIncomingCallListener(this)
        audioDeviceManager.addAudioDeviceEventsListener(this)
    }

    fun selectAudioDevice(audioDevice: AudioDevice) {
        audioDeviceManager.selectAudioDevice(audioDevice)
    }

    override fun onIncomingCall(call: ICall, video: Boolean, headers: Map<String?, String?>?) {
        if (callExists) {
            // App will reject incoming calls if already have one, because it supports only single managed call at a time.
            try {
                call.reject(RejectMode.DECLINE, null)
            } catch (e: CallException) {
                Log.e(APP_TAG, "AudioCallManager::onIncomingCall ${e.message}")
            }
            return
        }
        setCallState(CallState.INCOMING)
        call.apply {
            addCallListener(this@AudioCallManager)
            managedCall = this
            endpointUsername = endpoints.firstOrNull()?.userName
            endpointDisplayName = endpoints.firstOrNull()?.userDisplayName
        }
    }

    override fun onCallConnected(call: ICall?, headers: Map<String?, String?>?) {
        Log.i(APP_TAG, "AudioCallManager:: onCallConnected")
        setCallState(CallState.CONNECTED)
        endpointDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
        onCallConnect?.invoke()
        call?.let { startCallTimer(it) }
        playConnectedTone()
        startForegroundCallService()
    }

    override fun onCallAudioStarted(call: ICall?) {
        Log.i(APP_TAG, "AudioCallManager::onCallAudioStarted")
        stopProgressTone()
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean,
    ) {
        Log.i(APP_TAG, "AudioCallManager::onCallDisconnected answeredElsewhere: $answeredElsewhere")
        setCallState(CallState.DISCONNECTED)
        removeCall()
        onCallDisconnect?.invoke(false, context.getString(R.string.call_state_disconnected))
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?,
    ) {
        Log.i(APP_TAG, "AudioCallManager::onCallFailed code: $code, description: $description")
        removeCall()
        setCallState(CallState.FAILED)
        playFailedTone()
        onCallDisconnect?.invoke(true, description)
    }

    override fun onCallRinging(call: ICall?, headers: Map<String, String>?) {
        Log.d(APP_TAG, "AudioCallManager::onCallRinging")
        setCallState(CallState.RINGING)
        playProgressTone()
    }

    override fun onCallReconnecting(call: ICall?) {
        Log.d(APP_TAG, "AudioCallManager::onCallReconnecting")
        setCallState(CallState.RECONNECTING)
        _callTimer.cancel()
        stopProgressTone()
        if (_previousCallState.value in arrayOf(CallState.RINGING, CallState.CONNECTED) && _onHold.value == false) {
            playReconnectingTone()
        }
    }

    override fun onCallReconnected(call: ICall?) {
        Log.d(APP_TAG, "AudioCallManager::onCallReconnected")
        stopReconnectingTone()
        when (_previousCallState.value) {
            CallState.CONNECTING -> {
                setCallState(CallState.CONNECTING)
            }

            CallState.RINGING -> {
                setCallState(CallState.RINGING)
                playProgressTone()
            }

            CallState.CONNECTED -> {
                setCallState(CallState.CONNECTED)
                if (_onHold.value == false) {
                    call?.let { startCallTimer(it) }
                    playConnectedTone()
                }
            }

            CallState.INCOMING -> {
                setCallState(CallState.RECONNECTING)
            }

            else -> {
                _previousCallState.value?.let { setCallState(it) }
            }
        }
    }

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) = endpoint.setEndpointListener(this)

    @Throws(CallManagerException::class)
    open fun createOutgoingCall(user: String) = executeOrThrow {
        // App won't start new call if already have one, because it supports only single managed call at a time.
        if (callExists) {
            throw alreadyManagingCallError
        }
        setCallState(CallState.OUTGOING)
        managedCall = client.call(user, callSettings)?.apply {
            endpointUsername = user
            addCallListener(this@AudioCallManager)
        } ?: throw callCreationError
    }

    abstract fun startOutgoingCall()

    @Throws(CallManagerException::class)
    protected fun startOutgoingCallInternal() = executeOrThrow {
        managedCall?.start() ?: throw noActiveCallError
        _callDuration.postValue(0)
        setCallState(CallState.CONNECTING)
    }

    @Throws(CallManagerException::class)
    open fun answerIncomingCall() = executeOrThrow {
        if (_callState.value == CallState.CONNECTING) {
            return@executeOrThrow
        }
        notificationHelper.cancelIncomingCallNotification()
        _callDuration.postValue(0)
        if (callState.value == CallState.RECONNECTING) {
            //  setActive() is required here to notify wearable devices that the incoming call has been answered.
            playReconnectingTone()
        } else {
            setCallState(CallState.CONNECTING)
        }
        onCallAnswer?.invoke()
        managedCall?.answer(callSettings) ?: throw noActiveCallError
    }

    @Throws(CallManagerException::class)
    fun declineIncomingCall() = executeOrThrow {
        notificationHelper.cancelIncomingCallNotification()
        setCallState(CallState.DISCONNECTING)
        managedCall?.reject(RejectMode.DECLINE, null) ?: throw noActiveCallError
    }

    @Throws(CallManagerException::class)
    fun muteOngoingCall(mute: Boolean) = executeOrThrow {
        managedCall?.sendAudio(!mute).also { _muted.postValue(mute) } ?: throw noActiveCallError
    }

    open fun holdOngoingCall(hold: Boolean, onCompletion: () -> Unit = {}) = managedCall?.hold(
        hold,
        object : ICallCompletionHandler {
            override fun onComplete() {
                onCompletion()
                _onHold.postValue(hold)
                if (hold) {
                    _callTimer.cancel()
                } else {
                    managedCall?.let { startCallTimer(it) }
                }
                notificationHelper.updateOngoingNotification(userName = endpointUsername, callState = _callState.value, isOnHold = hold)
            }

            override fun onFailure(e: CallException) {}
        },
    )

    @Throws(CallManagerException::class)
    open fun hangupOngoingCall() = executeOrThrow {
        setCallState(CallState.DISCONNECTING)
        _callTimer.cancel()
        managedCall?.hangup(null) ?: throw noActiveCallError
    }

    @Throws(CallManagerException::class)
    fun sendDTMF(DTMF: String) = executeOrThrow {
        managedCall?.sendDTMF(DTMF) ?: throw noActiveCallError
    }

    @Throws(CallManagerException::class)
    protected fun executeOrThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: CallException) {
            throw callManagerException(e)
        } catch (e: CallManagerException) {
            throw e
        } catch (e: Exception) {
            throw CallManagerException(e.message ?: "An internal error")
        }
    }

    private fun removeCall() {
        stopForegroundService()
        notificationHelper.cancelIncomingCallNotification()
        notificationHelper.cancelOngoingCallNotification()
        managedCall?.removeCallListener(this)
        managedCall?.endpoints?.firstOrNull()?.setEndpointListener(null)
        managedCall = null
        endpointDisplayName = null
        endpointUsername = null
        _callTimer.cancel()
        _callTimer.purge()
        _muted.postValue(false)
        _onHold.postValue(false)
        stopProgressTone()
        stopReconnectingTone()
    }

    protected fun startCallTimer(call: ICall) {
        _callTimer = Timer("callTimer").apply {
            scheduleAtFixedRate(delay = TIMER_DELAY_MS, TIMER_DELAY_MS) {
                _callDuration.postValue(call.callDuration)
            }
        }
    }

    private fun setCallState(newState: CallState) {
        if (_callState.value != newState) {
            Log.d(APP_TAG, "AudioCallManager::setCallState: CallState ${_callState.value} changed to $newState")
            _previousCallState.postValue(_callState.value)
            _callState.tryEmit(newState)

            // Update notification
            if (newState in arrayOf(CallState.CONNECTED, CallState.RECONNECTING)) {
                _onHold.value?.let {
                    notificationHelper.updateOngoingNotification(userName = endpointUsername, callState = newState, isOnHold = it)
                }
            }
        }
    }

    /// Service

    private fun startForegroundCallService() {
        Handler(Looper.getMainLooper()).post {
            val filter = IntentFilter().apply {
                addAction(ACTION_HANGUP_ONGOING_CALL)
            }
            ContextCompat.registerReceiver(context, callBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            notificationHelper.createOngoingCallNotification(
                context,
                endpointDisplayName ?: endpointUsername,
                context.getString(R.string.call_in_progress),
                MainActivity::class.java,
            )

            Intent(context, CallService::class.java).apply {
                action = ACTION_FOREGROUND_SERVICE_AUDIO_CALL_START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(this)
                } else {
                    context.startService(this)
                }
            }
        }
    }

    private fun stopForegroundService() {
        Intent(context, CallService::class.java).apply {
            action = ACTION_FOREGROUND_SERVICE_STOP
            context.stopService(this)
        }
    }

    /// Incoming call visuals

    abstract fun showIncomingCallUI()

    protected fun showIncomingCallNotification() {
        Intent(context, CallActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val filter = IntentFilter().apply {
                addAction(ACTION_ANSWER_INCOMING_CALL)
                addAction(ACTION_DECLINE_INCOMING_CALL)
            }
            ContextCompat.registerReceiver(context, callBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            notificationHelper.showIncomingCallNotification(
                context,
                intent,
                endpointDisplayName ?: context.getString(R.string.unknown_user),
            )
        }
    }

    fun showIncomingCallFragment(answer: Boolean = false) {
        Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(IS_INCOMING_CALL, true)
            putExtra(ACTION_ANSWER_INCOMING_CALL, answer)
            startActivity(context, this, null)
        }
    }

    private fun playProgressTone() {
        callProgressToneFile?.play(true)
    }

    private fun stopProgressTone() {
        callProgressToneFile?.stop(false)
    }

    private fun playReconnectingTone() {
        callReconnectingToneFile?.play(true)
    }

    private fun stopReconnectingTone() {
        callReconnectingToneFile?.stop(false)
    }

    private fun playConnectedTone() {
        callConnectedToneFile?.play(false)
    }

    private fun playFailedTone() {
        callFailedToneFile?.play(false)
    }

    override fun onAudioDeviceChanged(currentAudioDevice: AudioDevice?) {
        Log.d(APP_TAG, "AudioCallManager::onAudioDeviceChanged::currentAudioDevice: $currentAudioDevice")
        _selectedAudioDevice.value = currentAudioDevice
    }

    override fun onAudioDeviceListChanged(newDeviceList: MutableList<AudioDevice>?) {
        Log.d(APP_TAG, "AudioCallManager::onAudioDeviceListChanged::newDeviceList: $newDeviceList")
        _availableAudioDevices.value = newDeviceList
    }
}

private class AudioCallActionReceiver(private val onReceive: (AudioCallActionReceiver) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = onReceive(this)
}

class AudioCallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECLINE_INCOMING_CALL -> audioCallManager.declineIncomingCall()
            ACTION_HANGUP_ONGOING_CALL -> audioCallManager.hangupOngoingCall()
        }
    }
}
