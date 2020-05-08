package com.voximplant.demos.kotlin.video_call.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.voximplant.demos.kotlin.video_call.stories.incoming_call.IncomingCallActivity
import com.voximplant.demos.kotlin.video_call.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientIncomingCallListener
import com.voximplant.sdk.hardware.AudioDevice
import org.webrtc.VideoSink
import java.lang.Exception

private class CallActionReceiver(private val onReceive: (CallActionReceiver) -> Unit) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onReceive(this)
        }
    }
}

typealias VideoStreamAdded = (local: Boolean, completion: (VideoSink) -> Unit) -> Unit
typealias VideoStreamRemoved = (local: Boolean) -> Unit

class VoximplantCallManager(
    private val client: IClient,
    private val appContext: Context,
    private val notificationHelper: NotificationHelper
): IClientIncomingCallListener, ICallListener, IEndpointListener {

    private var managedCall: ICall? = null
    private val callActionsReceiver = CallActionReceiver {
        notificationHelper.cancelNotification()
        declineCall()
        appContext.unregisterReceiver(it)
    }
    private val audioDeviceManager = Voximplant.getAudioDeviceManager()
    val selectedAudioDevice: AudioDevice get() = audioDeviceManager.activeDevice
    val availableAudioDevices: MutableList<AudioDevice> get() = audioDeviceManager.audioDevices

    var latestCallDisplayName: String? = null
        private set
    var latestCallUsername: String? = null
        private set
    var videoStreamAdded: VideoStreamAdded? = null
    var videoStreamRemoved: VideoStreamRemoved? = null
    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null

    init {
        client.setClientIncomingCallListener(this)
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
            latestCallUsername = it.endpoints.firstOrNull()?.userName
            latestCallDisplayName = it.endpoints.firstOrNull()?.userDisplayName
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
            latestCallUsername = user
            it.addCallListener(this)
        } ?: throw callCreationError
    }

    @Throws(CallManagerException::class)
    fun startCall() = executeOrThrow { managedCall?.start() ?: throw noActiveCallError }

    private val callSettings: CallSettings
        get() = CallSettings().apply {
            videoFlags = VideoFlags(true, true)
        }

    @Throws(CallManagerException::class)
    fun answerCall() = executeOrThrow { managedCall?.answer(callSettings) ?: throw noActiveCallError }

    @Throws(CallManagerException::class)
    fun declineCall() = executeOrThrow { declineCall(managedCall ?: throw  noActiveCallError) }

    @Throws(CallManagerException::class)
    fun muteActiveCall(mute: Boolean) = executeOrThrow { managedCall?.sendAudio(!mute) ?: throw noActiveCallError }

    fun selectAudioDevice(name: String) =
        audioDeviceManager.audioDevices.first { it.name == name }?.let {
            audioDeviceManager.selectAudioDevice(it)
        }

    fun holdActiveCall(hold: Boolean, completion: (CallManagerException?) -> Unit) =
        managedCall?.hold(hold, object : ICallCompletionHandler {
            override fun onComplete() {
                completion(null)
            }
            override fun onFailure(e: CallException) {
                completion(callManagerException(e))
            }
        }) ?: completion(noActiveCallError)


    fun sendVideo(send: Boolean, completion: (CallManagerException?) -> Unit) =
        managedCall?.sendVideo(send, object: ICallCompletionHandler {
            override fun onComplete() {
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
        managedCall?.removeCallListener(this)
        managedCall = null
    }

    override fun onCallConnected(
        call: ICall?,
        headers: Map<String?, String?>?
    ) {
        startForegroundCallService()
        notificationHelper.cancelNotification()
        onCallConnect?.invoke()
        latestCallDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean
    ) {
        stopForegroundService()
        notificationHelper.cancelNotification()
        removeCall()
        call.endpoints.firstOrNull()?.setEndpointListener(null)
        onCallDisconnect?.invoke(false, "Disconnected")
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?
    ) {
        stopForegroundService()
        notificationHelper.cancelNotification()
        removeCall()
        call.endpoints.firstOrNull()?.setEndpointListener(null)
        onCallDisconnect?.invoke(true, description)
    }

    private fun startForegroundCallService() {
        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_START
            appContext.startService(it)
        }
    }

    override fun onLocalVideoStreamAdded(call: ICall, videoStream: IVideoStream) {
        Thread.sleep(1000)
        videoStreamAdded?.invoke(true) { videoSink ->
            videoStream.addVideoRenderer(videoSink, RenderScaleType.SCALE_FIT)
        }
    }

    override fun onLocalVideoStreamRemoved(call: ICall?, videoStream: IVideoStream?) {
        videoStreamRemoved?.invoke(true)
    }

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) {
        endpoint.setEndpointListener(this)
    }

    override fun onRemoteVideoStreamAdded(endpoint: IEndpoint, videoStream: IVideoStream) {
        videoStreamAdded?.invoke(false) { videoSink ->
            videoStream.addVideoRenderer(videoSink, RenderScaleType.SCALE_FIT)
        }
    }

    override fun onRemoteVideoStreamRemoved(endpoint: IEndpoint?, videoStream: IVideoStream?) {
        videoStreamRemoved?.invoke(false)
    }

    private fun stopForegroundService() {
        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_STOP
            appContext.stopService(it)
        }
    }

    private fun presentIncomingCallUI() {
        Intent(appContext, IncomingCallActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Shared.foregroundCheck.isInForeground) {
                appContext.registerReceiver(callActionsReceiver, IntentFilter(ACTION_DECLINE_CALL))
                notificationHelper.showCallNotification(
                    appContext,
                    intent,
                    latestCallDisplayName ?: latestCallUsername.orEmpty()
                )
            } else {
                appContext.startActivity(intent)
            }
        }
    }
}