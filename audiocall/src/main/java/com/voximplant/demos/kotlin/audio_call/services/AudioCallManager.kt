/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.audio_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.audio_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.audio_call.telecomManager
import com.voximplant.demos.kotlin.services.CallService
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.notificationHelper
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientIncomingCallListener
import com.voximplant.sdk.hardware.AudioFileUsage
import com.voximplant.sdk.hardware.IAudioFile
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class AudioCallManager(
    private val appContext: Context,
    private val client: IClient,
) : IClientIncomingCallListener, ICallListener, IEndpointListener {

    // Call management
    private var managedCall: ICall? = null
    val callExists: Boolean
        get() = managedCall != null
    val callerDisplayName
        get() = managedCall?.endpoints?.get(0)?.userDisplayName
    var managedCallConnection: CallConnection? = null
    private val _callState = MutableLiveData(CallState.NONE)
    val callState: LiveData<CallState>
        get() = _callState
    private val _previousCallState = MutableLiveData(CallState.NONE)
    private var _callTimer: Timer = Timer("callTimer")
    private val _callDuration = MutableLiveData(0L)
    val callDuration: LiveData<Long>
        get() = _callDuration
    val callBroadcastReceiver: BroadcastReceiver = AudioCallBroadcastReceiver()
    private val callSettings: CallSettings
        get() = CallSettings().apply { videoFlags = VideoFlags(false, false) }

    // Call properties
    var latestCallerDisplayName: String? = null
        private set
    var latestCallerUsername: String? = null
        private set
    private val _muted = MutableLiveData(false)
    val muted: LiveData<Boolean>
        get() = _muted
    private val _onHold = MutableLiveData(false)
    val onHold: LiveData<Boolean>
        get() = _onHold

    private var callProgressToneFile: IAudioFile? = Voximplant.createAudioFile(appContext, R.raw.call_progress_tone, AudioFileUsage.IN_CALL)
    private var callReconnectingToneFile: IAudioFile? = Voximplant.createAudioFile(appContext, R.raw.call_reconnecting_tone, AudioFileUsage.IN_CALL)
    private var callConnectedToneFile: IAudioFile? = Voximplant.createAudioFile(appContext, R.raw.call_connected_tone, AudioFileUsage.IN_CALL)
    private var callFailedToneFile: IAudioFile? = Voximplant.createAudioFile(appContext, R.raw.call_failed_tone, AudioFileUsage.IN_CALL)

    // Call events
    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null

    init {
        client.setClientIncomingCallListener(this)
    }

    fun createConnection(): CallConnection? {
        managedCallConnection = CallConnection()
        managedCallConnection?.setInitialized()
        Log.i(APP_TAG, "AudioCallManager::createConnection: Connection created & initialized")
        return managedCallConnection
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
        call.also {
            it.addCallListener(this)
            managedCall = it
            latestCallerUsername = it.endpoints.firstOrNull()?.userName
            latestCallerDisplayName = it.endpoints.firstOrNull()?.userDisplayName
        }
        telecomManager.addIncomingCall()
    }

    override fun onCallConnected(call: ICall?, headers: Map<String?, String?>?) {
        managedCallConnection?.setActive()
        setCallState(CallState.CONNECTED)
        onCallConnect?.invoke()
        call?.let { startCallTimer(it) }
        playConnectedTone()
        latestCallerDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
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
        removeCall()
        when {
            answeredElsewhere -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE))
            }
            _callState.value == CallState.DISCONNECTING -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            }
            _callState.value == CallState.CONNECTED -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            }
            _callState.value == CallState.INCOMING -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
            }
            else -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.OTHER))
            }
        }
        setCallState(CallState.DISCONNECTED)
        managedCallConnection?.destroy()
        onCallDisconnect?.invoke(false, appContext.getString(R.string.call_state_disconnected))
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?,
    ) {
        Log.i(APP_TAG, "AudioCallManager::onCallFailed code: $code, description: $description")
        removeCall()
        when (code) {
            486 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.BUSY))
            603 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            else -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
        }
        setCallState(CallState.FAILED)
        playFailedTone()
        managedCallConnection?.destroy()
        onCallDisconnect?.invoke(true, description)
    }

    override fun onCallRinging(call: ICall?, headers: Map<String, String>?) {
        Log.d(APP_TAG, "AudioCallManager::onCallRinging")
        setCallState(CallState.RINGING)
        managedCallConnection?.setDialing()
        playProgressTone()
    }

    override fun onCallReconnecting(call: ICall?) {
        Log.d(APP_TAG, "AudioCallManager::onCallReconnecting")
        setCallState(CallState.RECONNECTING)
        _callTimer.cancel()
        stopProgressTone()
        if (_previousCallState.value in arrayOf(CallState.RINGING, CallState.CONNECTED)) {
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
                }
                playConnectedTone()
            }
            CallState.INCOMING -> {
                setCallState(CallState.RECONNECTING)
            }
            else -> {
                _previousCallState.value?.let { setCallState(it) }
            }
        }
    }

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) =
        endpoint.setEndpointListener(this)

    @Throws(CallManagerException::class)
    fun createOutgoingCall(user: String) =
        executeOrThrow {
            // App won't start new call if already have one, because it supports only single managed call at a time.
            if (callExists) {
                throw alreadyManagingCallError
            }
            setCallState(CallState.OUTGOING)
            managedCall = client.call(user, callSettings)?.also {
                latestCallerUsername = user
                telecomManager.addOutgoingCall(user)
                it.addCallListener(this)
            }
                ?: throw callCreationError
        }

    @Throws(CallManagerException::class)
    fun startOutgoingCall() =
        executeOrThrow {
            _callDuration.postValue(0)
            setCallState(CallState.CONNECTING)
            managedCall?.start() ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun answerIncomingCall() =
        executeOrThrow {
            notificationHelper.cancelIncomingCallNotification()
            _callDuration.postValue(0)
            if (callState.value == CallState.RECONNECTING) {
                playReconnectingTone()
            } else {
                setCallState(CallState.CONNECTING)
            }
            managedCall?.answer(callSettings)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun declineIncomingCall() =
        executeOrThrow {
            notificationHelper.cancelIncomingCallNotification()
            setCallState(CallState.DISCONNECTING)
            managedCall?.reject(RejectMode.DECLINE, null)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun muteOngoingCall(mute: Boolean) =
        executeOrThrow {
            managedCall?.sendAudio(!mute).also { _muted.postValue(mute) }
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun holdOngoingCall(hold: Boolean) {
        executeOrThrow {
            managedCall?.hold(hold, object : ICallCompletionHandler {
                override fun onComplete() {
                    _onHold.postValue(hold)
                    if (hold) {
                        managedCallConnection?.setOnHold()
                        _callTimer.cancel()
                    } else {
                        managedCallConnection?.setActive()
                        managedCall?.let { startCallTimer(it) }
                    }
                    _callState.value?.let {
                        notificationHelper.updateOngoingNotification(userName = latestCallerUsername, callState = it, isOnHold = hold)
                    }
                }

                override fun onFailure(e: CallException) {}
            }) ?: throw noActiveCallError
        }
    }

    @Throws(CallManagerException::class)
    fun hangupOngoingCall() =
        executeOrThrow {
            managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            setCallState(CallState.DISCONNECTING)
            _callTimer.cancel()
            managedCall?.hangup(null)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun sendDTMF(DTMF: String) =
        executeOrThrow {
            managedCall?.sendDTMF(DTMF)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    private fun executeOrThrow(executable: () -> Unit) {
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
        _callTimer.cancel()
        _callTimer.purge()
        _muted.postValue(false)
        _onHold.postValue(false)
        stopProgressTone()
        stopReconnectingTone()
    }

    private fun startCallTimer(call: ICall) {
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
            _callState.postValue(newState)

            // Update notification
            if (newState in arrayOf(CallState.CONNECTED, CallState.RECONNECTING)) {
                _onHold.value?.let {
                    notificationHelper.updateOngoingNotification(userName = latestCallerUsername, callState = newState, isOnHold = it)
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
            appContext.registerReceiver(callBroadcastReceiver, filter)
            notificationHelper.createOngoingCallNotification(
                appContext,
                latestCallerDisplayName ?: latestCallerUsername,
                appContext.getString(R.string.call_in_progress),
                MainActivity::class.java,
            )

            Intent(appContext, CallService::class.java).let {
                it.action = ACTION_FOREGROUND_SERVICE_START
                appContext.startService(it)
            }
        }
    }

    private fun stopForegroundService() {
        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_STOP
            appContext.stopService(it)
        }
    }

    /// Incoming call visuals

    fun showIncomingCallUI() {
        if (callExists) {
            showIncomingCallNotification()
            if (Shared.appInForeground) {
                showIncomingCallFragment()
            }
        } else {
            managedCallConnection?.destroy()
        }
    }

    private fun showIncomingCallNotification() {
        Intent(appContext, CallActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val filter = IntentFilter().apply {
                addAction(ACTION_ANSWER_INCOMING_CALL)
                addAction(ACTION_DECLINE_INCOMING_CALL)
            }
            appContext.registerReceiver(callBroadcastReceiver, filter)
            notificationHelper.showIncomingCallNotification(
                appContext,
                intent,
                latestCallerDisplayName ?: appContext.getString(R.string.unknown_user),
            )
        }
    }

    fun showIncomingCallFragment(answer: Boolean = false) {
        Intent(appContext, CallActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(IS_INCOMING_CALL, true)
            it.putExtra(ACTION_ANSWER_INCOMING_CALL, answer)
            startActivity(appContext, it, null)
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

}

private class AudioCallActionReceiver(private val onReceive: (AudioCallActionReceiver) -> Unit) :
    BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) = onReceive(this)
}

class AudioCallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_INCOMING_CALL -> audioCallManager.showIncomingCallFragment(answer = true)
            ACTION_DECLINE_INCOMING_CALL -> audioCallManager.declineIncomingCall()
            ACTION_HANGUP_ONGOING_CALL -> audioCallManager.hangupOngoingCall()
        }
    }
}
