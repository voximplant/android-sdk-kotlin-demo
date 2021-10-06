/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.IAudioDeviceEventsListener
import com.voximplant.sdk.hardware.ICustomVideoSource
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.VideoSink

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
    val callBroadcastReceiver: BroadcastReceiver = CallBroadcastReceiver()
    private val audioDeviceManager = Voximplant.getAudioDeviceManager()
    var customVideoSource: ICustomVideoSource? = null

    private val _selectedAudioDevice = MutableLiveData(audioDeviceManager.activeDevice)
    val selectedAudioDevice: LiveData<AudioDevice> = _selectedAudioDevice
    val availableAudioDevices: MutableList<AudioDevice> get() = audioDeviceManager.audioDevices

    var latestCallerDisplayName: String? = null
        private set
    var latestCallerUsername: String? = null
        private set
    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null

    var muted: Boolean = false
        private set
    var onHold: Boolean = false
        private set
    var sharingScreen: Boolean = false
        private set

    var showLocalVideoView = MutableLiveData(false)
        private set
    var showRemoteVideoView = MutableLiveData(false)
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
        if (managedCall != null) {
            // App will reject incoming calls if already have one, because it supports only single managed call at a time.
            try {
                declineCall(call)
            } catch (e: CallException) {
                Log.e(APP_TAG, e.message ?: "")
            }
            return
        }
        call.also {
            it.addCallListener(this)
            managedCall = it
            latestCallerDisplayName = it.endpoints.firstOrNull()?.userName
            latestCallerUsername = it.endpoints.firstOrNull()?.userDisplayName
            presentIncomingCallUI()
        }
    }

    @Throws(CallManagerException::class)
    fun createCall(user: String) = executeOrThrow {
        // App won't start new call if already have one, because it supports only single managed call at a time.
        if (managedCall != null) {
            throw alreadyManagingCallError
        }
        managedCall = client.call(user, callSettings)?.also {
            latestCallerUsername = user
            it.addCallListener(this)
        } ?: throw callCreationError
    }

    @Throws(CallManagerException::class)
    fun startCall() = executeOrThrow { managedCall?.start() ?: throw noActiveCallError }

    private val callSettings: CallSettings
        get() = CallSettings().also {
            it.videoFlags = videoFlags
        }

    @Throws(CallManagerException::class)
    fun answerCall() =
        executeOrThrow { managedCall?.answer(callSettings) ?: throw noActiveCallError }

    @Throws(CallManagerException::class)
    fun declineCall() = executeOrThrow { declineCall(managedCall ?: throw  noActiveCallError) }

    @Throws(CallManagerException::class)
    fun muteActiveCall(mute: Boolean) = executeOrThrow {
        managedCall?.sendAudio(!mute).also {
            muted = mute
        }
            ?: throw noActiveCallError
    }

    fun holdActiveCall(hold: Boolean, completion: (CallManagerException?) -> Unit) {

        managedCall?.hold(hold, object : ICallCompletionHandler {
            override fun onComplete() {
                onHold = !onHold
                if (hasLocalVideoStream) showLocalVideoView.postValue(!onHold)
                if (hasRemoteVideoStream) showRemoteVideoView.postValue(!onHold)
                completion(null)
            }

            override fun onFailure(e: CallException) {
                completion(callManagerException(e))
            }
        }) ?: completion(noActiveCallError)
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
    fun hangup() = executeOrThrow { managedCall?.hangup(null) ?: throw noActiveCallError }

    @Throws(CallManagerException::class)
    private fun declineCall(call: ICall) = executeOrThrow { call.reject(RejectMode.DECLINE, null) }

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
        headers: Map<String?, String?>?
    ) {
        Shared.notificationHelper.cancelIncomingCallNotification()
        onCallConnect?.invoke()
        latestCallerDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
        startForegroundCallService()
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean
    ) {
        showLocalVideoView.postValue(false)
        showRemoteVideoView.postValue(false)
        removeCall()
        onCallDisconnect?.invoke(false, appContext.getString(R.string.disconnected))
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?
    ) {
        removeCall()
        onCallDisconnect?.invoke(true, description)
    }

    private fun startForegroundCallService() {
        val filter = IntentFilter().apply {
            addAction(ACTION_HANGUP_ONGOING_CALL)
        }
        appContext.registerReceiver(callBroadcastReceiver, filter)
        Shared.notificationHelper.createOngoingCallNotification(
            appContext,
            latestCallerDisplayName ?: latestCallerUsername,
            appContext.getString(R.string.call_in_progress),
            callActivity,
        )

        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_START
            appContext.startService(it)
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

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) {
        endpoint.setEndpointListener(this)
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
                latestCallerDisplayName ?: latestCallerUsername.orEmpty()
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

}

class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_INCOMING_CALL ->
                Shared.voximplantCallManager.showIncomingCallActivity(answer = true)
            ACTION_DECLINE_INCOMING_CALL -> Shared.voximplantCallManager.declineCall()
            ACTION_HANGUP_ONGOING_CALL -> Shared.voximplantCallManager.hangup()
        }
    }
}