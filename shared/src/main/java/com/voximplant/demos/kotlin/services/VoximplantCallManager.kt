/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientIncomingCallListener
import com.voximplant.sdk.hardware.*
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoSink
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

private class CallActionReceiver(private val onReceive: (CallActionReceiver) -> Unit) :
    BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onReceive(this)
        }
    }
}

typealias localStreamRendering = (videoStream: ILocalVideoStream, added: Boolean) -> Unit
typealias remoteStreamRendering = (videoStream: IRemoteVideoStream, added: Boolean) -> Unit

class VoximplantCallManager(
    private val appContext: Context,
    private val client: IClient,
    private val videoFlags: VideoFlags,
    private val callActivity: Class<*>,
    private val incomingCallActivity: Class<*>,
) : IClientIncomingCallListener, ICallListener, IEndpointListener, IAudioDeviceEventsListener {

    var managedCall: ICall? = null
    val callExists: Boolean
        get() = managedCall != null
    private val _callState = MutableLiveData(CallState.NONE)
    val callState: LiveData<CallState>
        get() = _callState
    private val _previousCallState = MutableLiveData(CallState.NONE)
    private var _callTimer: Timer = Timer("callTimer")
    private val _callDuration = MutableLiveData(0L)
    val callDuration: LiveData<Long>
        get() = _callDuration
    val callBroadcastReceiver: BroadcastReceiver = CallBroadcastReceiver()
    private val callSettings: CallSettings
        get() = CallSettings().also { it.videoFlags = videoFlags }
    private val audioDeviceManager = Voximplant.getAudioDeviceManager()
    var customVideoSource: ICustomVideoSource? = null

    private val _selectedAudioDevice = MutableLiveData(audioDeviceManager.activeDevice)
    val selectedAudioDevice: LiveData<AudioDevice> = _selectedAudioDevice
    val availableAudioDevices: MutableList<AudioDevice> get() = audioDeviceManager.audioDevices

    var endpointDisplayName: String? = null
        private set
    var endpointUsername: String? = null
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

    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null

    var sharingScreen: Boolean = false
        private set
    var showLocalVideoView = MutableLiveData(false)
        private set
    var showRemoteVideoView = MutableLiveData(false)
        private set
    var remoteVideoIsPortrait = MutableLiveData<Boolean>()
        private set

    val localVideoRenderer = MutableLiveData<(VideoSink) -> Unit>()
    val remoteVideoRenderer = MutableLiveData<(VideoSink) -> Unit>()

    private var changeLocalStream: localStreamRendering? = null
    private var changeRemoteStream: remoteStreamRendering? = null
    private var localVideoStream: ILocalVideoStream? = null
    private var remoteVideoStream: IRemoteVideoStream? = null
    val hasLocalVideoStream: Boolean
        get() = localVideoStream != null
    private val hasRemoteVideoStream: Boolean
        get() = remoteVideoStream != null

    var setRenderSurface: ((surface: Surface?, size: Size) -> Unit)? = null

    init {
        client.setClientIncomingCallListener(this)

        audioDeviceManager.addAudioDeviceEventsListener(this)
    }

    fun releaseCustomVideoSource() {
        customVideoSource?.release()
    }

    override fun onIncomingCall(
        call: ICall,
        video: Boolean,
        headers: Map<String?, String?>?
    ) {
        if (callExists) {
            // App will reject incoming calls if already have one, because it supports only single managed call at a time.
            try {
                call.reject(RejectMode.DECLINE, null)
            } catch (e: CallException) {
                Log.e(APP_TAG, "VoximplantCallManager::onIncomingCall ${e.message}")
            }
            return
        }
        setCallState(CallState.INCOMING)
        call.also {
            it.addCallListener(this)
            managedCall = it
            endpointDisplayName = it.endpoints.firstOrNull()?.userDisplayName
            endpointUsername = it.endpoints.firstOrNull()?.userName
            presentIncomingCallUI()
        }
    }

    override fun onCallRinging(call: ICall?, headers: Map<String, String>?) {
        Log.d(APP_TAG, "VoximplantCallManager::onCallRinging")
        setCallState(CallState.RINGING)
        playProgressTone()
    }

    override fun onCallReconnecting(call: ICall?) {
        Log.d(APP_TAG, "VoximplantCallManager::onCallReconnecting")
        setCallState(CallState.RECONNECTING)
        _callTimer.cancel()
        stopProgressTone()
        if (_previousCallState.value in arrayOf(CallState.RINGING, CallState.CONNECTED)) {
            playReconnectingTone()
        }
    }

    override fun onCallReconnected(call: ICall?) {
        Log.d(APP_TAG, "VoximplantCallManager::onCallReconnected")
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
            else -> {
                _previousCallState.value?.let { setCallState(it) }
            }
        }
    }

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) =
        endpoint.setEndpointListener(this)

    @Throws(CallManagerException::class)
    fun createCall(user: String) =
        executeOrThrow {
            // App won't start new call if already have one, because it supports only single managed call at a time.
            if (callExists) {
                throw alreadyManagingCallError
            }
            setCallState(CallState.OUTGOING)
            managedCall = client.call(user, callSettings)?.also {
                endpointUsername = user
                it.addCallListener(this)
            }
                ?: throw callCreationError
        }

    @Throws(CallManagerException::class)
    fun startCall() =
        executeOrThrow {
            _callDuration.postValue(0)
            setCallState(CallState.CONNECTING)
            managedCall?.start()
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun answerCall() =
        executeOrThrow {
            _callDuration.postValue(0)
            setCallState(CallState.CONNECTING)
            managedCall?.answer(callSettings)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun declineCall() =
        executeOrThrow {
            Shared.notificationHelper.cancelIncomingCallNotification()
            setCallState(CallState.DISCONNECTING)
            managedCall?.reject(RejectMode.DECLINE, null)
                ?: throw  noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun muteActiveCall(mute: Boolean) = executeOrThrow {
        managedCall?.sendAudio(!mute).also { _muted.postValue(mute) }
            ?: throw noActiveCallError
    }

    @Throws(CallManagerException::class)
    fun holdActiveCall(hold: Boolean) {
        executeOrThrow {
            managedCall?.hold(hold, object : ICallCompletionHandler {
                override fun onComplete() {
                    _onHold.postValue(hold)
                    if (hasLocalVideoStream) showLocalVideoView.postValue(!hold)
                    if (hasRemoteVideoStream) showRemoteVideoView.postValue(!hold)
                    if (hold) {
                        _callTimer.cancel()
                    } else {
                        managedCall?.let { startCallTimer(it) }
                    }
                    _callState.value?.let {
                        Shared.notificationHelper.updateOngoingNotification(userName = endpointUsername, callState = it, isOnHold = hold)
                    }
                }

                override fun onFailure(e: CallException) {}
            }) ?: throw noActiveCallError
        }
    }

    fun selectAudioDevice(id: Int) {
        audioDeviceManager.selectAudioDevice(audioDeviceManager.audioDevices[id])
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun shareScreen(intent: Intent, completion: (CallManagerException?) -> Unit) =
        managedCall?.startScreenSharing(intent, object : ICallCompletionHandler {
            override fun onComplete() {
                sharingScreen = true
                completion(null)
            }

            override fun onFailure(e: CallException) {
                completion(callManagerException(e))
            }
        }) ?: completion(noActiveCallError)

    fun sendVideo(send: Boolean, completion: (CallManagerException?) -> Unit) =
        managedCall?.sendVideo(send, object : ICallCompletionHandler {
            override fun onComplete() {
                sharingScreen = false
                completion(null)
            }

            override fun onFailure(e: CallException) {
                completion(callManagerException(e))
            }
        }) ?: completion(noActiveCallError)

    @Throws(CallManagerException::class)
    fun hangup() =
        executeOrThrow {
            setCallState(CallState.DISCONNECTING)
            _callTimer.cancel()
            managedCall?.hangup(null)
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
        Shared.notificationHelper.cancelIncomingCallNotification()
        Shared.notificationHelper.cancelOngoingCallNotification()
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
                    Shared.notificationHelper.updateOngoingNotification(userName = endpointUsername, callState = newState, isOnHold = it)
                }
            }
        }
    }

    fun initVideoStreams() {
        changeLocalStream = { videoStream, added ->
            localVideoRenderer.postValue { videoSink ->
                if (added) {
                    addRenderer(
                        videoStream,
                        videoSink,
                        isLocal = true,
                    )
                } else {
                    videoStream.removeVideoRenderer(videoSink)
                    showLocalVideoView.postValue(false)
                }
            }
        }

        changeRemoteStream = { videoStream, added ->
            remoteVideoRenderer.postValue { videoSink ->
                if (added) {
                    addRenderer(
                        videoStream,
                        videoSink,
                        isLocal = false,
                    )
                } else {
                    videoStream.removeVideoRenderer(videoSink)
                    showRemoteVideoView.postValue(false)
                }
            }
        }
    }

    override fun onCallConnected(
        call: ICall?,
        headers: Map<String?, String?>?,
    ) {
        setCallState(CallState.CONNECTED)
        endpointDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
        onCallConnect?.invoke()
        Shared.notificationHelper.cancelIncomingCallNotification()
        call?.let { startCallTimer(it) }
        playConnectedTone()
        startForegroundCallService()
    }

    override fun onCallAudioStarted(call: ICall?) {
        Log.i(APP_TAG, "VoximplantCallManager::onCallAudioStarted")
        stopProgressTone()
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean,
    ) {
        Log.i(APP_TAG, "VoximplantCallManager::onCallDisconnected answeredElsewhere: $answeredElsewhere")
        setCallState(CallState.DISCONNECTED)
        removeCall()
        showLocalVideoView.postValue(false)
        showRemoteVideoView.postValue(false)
        onCallDisconnect?.invoke(false, appContext.getString(R.string.call_state_disconnected))
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?,
    ) {
        Log.i(APP_TAG, "VoximplantCallManager::onCallFailed code: $code, description: $description")
        removeCall()
        setCallState(CallState.FAILED)
        playFailedTone()
        onCallDisconnect?.invoke(true, description)
    }

    private fun startForegroundCallService() {
        Handler(Looper.getMainLooper()).post {
            val filter = IntentFilter().apply {
                addAction(ACTION_HANGUP_ONGOING_CALL)
            }
            appContext.registerReceiver(callBroadcastReceiver, filter)
            Shared.notificationHelper.createOngoingCallNotification(
                appContext,
                endpointDisplayName ?: endpointUsername,
                appContext.getString(R.string.call_in_progress),
                callActivity,
            )

            Intent(appContext, CallService::class.java).let {
                it.action = ACTION_FOREGROUND_SERVICE_START
                appContext.startService(it)
            }
        }
    }

    override fun onLocalVideoStreamAdded(call: ICall, videoStream: ILocalVideoStream) {
        if (videoStream.videoStreamType == VideoStreamType.SCREEN_SHARING) {
            return
        }
        localVideoStream = videoStream
        changeLocalStream?.invoke(videoStream, true)
    }

    override fun onLocalVideoStreamRemoved(call: ICall, videoStream: ILocalVideoStream) {
        localVideoStream = null
        changeLocalStream?.invoke(videoStream, false)
    }

    override fun onRemoteVideoStreamAdded(endpoint: IEndpoint, videoStream: IRemoteVideoStream) {
        remoteVideoStream = videoStream
        changeRemoteStream?.invoke(videoStream, true)
    }

    override fun onRemoteVideoStreamRemoved(
        endpoint: IEndpoint,
        videoStream: IRemoteVideoStream
    ) {
        showRemoteVideoView.postValue(false)
        remoteVideoStream = null
        changeRemoteStream?.invoke(videoStream, false)
    }

    override fun onAudioDeviceChanged(currentAudioDevice: AudioDevice?) {
        _selectedAudioDevice.postValue(currentAudioDevice)
    }

    override fun onAudioDeviceListChanged(newDeviceList: MutableList<AudioDevice>?) {}

    private fun stopForegroundService() {
        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_STOP
            appContext.stopService(it)
        }
    }

    private fun addRenderer(videoStream: IVideoStream, videoSink: VideoSink, isLocal: Boolean) {
        videoStream.addVideoRenderer(videoSink, RenderScaleType.SCALE_FIT, object : RendererEvents {
            override fun onFirstFrameRendered() {
                if (isLocal) {
                    showLocalVideoView.postValue(true)
                } else {
                    showRemoteVideoView.postValue(true)
                }
            }

            override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                Log.d(APP_TAG, "VoximplantCallManager::addRenderer: $videoWidth $videoHeight $rotation")
                if (!isLocal) {
                    if (rotation == 90 || rotation == 270) {
                        remoteVideoIsPortrait.postValue(true)
                    } else {
                        remoteVideoIsPortrait.postValue(false)
                    }
                }
            }
        })
    }

    private fun presentIncomingCallUI() {
        showIncomingCallNotification()
        if (Shared.appInForeground) {
            showIncomingCallActivity()
        }
    }

    private fun showIncomingCallNotification() {
        Intent(appContext, incomingCallActivity).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val filter = IntentFilter().apply {
                addAction(ACTION_ANSWER_INCOMING_CALL)
                addAction(ACTION_DECLINE_INCOMING_CALL)
            }
            appContext.registerReceiver(callBroadcastReceiver, filter)
            Shared.notificationHelper.showIncomingCallNotification(
                appContext,
                intent,
                endpointDisplayName ?: endpointUsername.orEmpty()
            )
        }
    }

    fun showIncomingCallActivity(answer: Boolean = false) {
        Intent(appContext, incomingCallActivity).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(IS_INCOMING_CALL, true)
            it.putExtra(ACTION_ANSWER_INCOMING_CALL, answer)
            ContextCompat.startActivity(appContext, it, null)
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

class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_INCOMING_CALL -> Shared.voximplantCallManager.showIncomingCallActivity(answer = true)
            ACTION_DECLINE_INCOMING_CALL -> Shared.voximplantCallManager.declineCall()
            ACTION_HANGUP_ONGOING_CALL -> Shared.voximplantCallManager.hangup()
        }
    }
}