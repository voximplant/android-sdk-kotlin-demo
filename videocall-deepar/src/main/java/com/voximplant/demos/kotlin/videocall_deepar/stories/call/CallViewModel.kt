package com.voximplant.demos.kotlin.videocall_deepar.stories.call

import ai.deepar.ar.CameraResolutionPreset
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.videocall_deepar.services.CameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.services.DeepARHelper
import com.voximplant.demos.kotlin.videocall_deepar.services.VoximplantCallManager
import com.voximplant.demos.kotlin.videocall_deepar.utils.*
import com.voximplant.sdk.call.RenderScaleType
import org.webrtc.VideoSink


class CallViewModel : BaseViewModel() {
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager
    private val deepARHelper: DeepARHelper = Shared.deepARHelper
    private val cameraHelper: CameraHelper = Shared.cameraHelper

    private val cameraPreset = CameraResolutionPreset.P640x480

    val muted = MutableLiveData<Boolean>()
    private var _muted: Boolean = false
        set(value) {
            field = value
            muted.postValue(value)
        }

    val sendingVideo = MutableLiveData<Boolean>()
    private var _sendingVideo: Boolean = true
        set(value) {
            field = value
            sendingVideo.postValue(value)
        }

    val receivingVideo = MutableLiveData<Boolean>()
    private var _receivingVideo: Boolean = true
        set(value) {
            field = value
            receivingVideo.postValue(value)
        }

    val availableAudioDevices: List<String>
        get() = callManager.availableAudioDevices.map { device ->
            if (device.name == callManager.selectedAudioDevice.name) {
                "${device.name} (Current)"
            } else {
                device.name
            }
        }

    val localVideoRenderer = MutableLiveData<(VideoSink) -> Unit>()
    val remoteVideoRenderer = MutableLiveData<(VideoSink) -> Unit>()

    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()
    val enableVideoButton = MutableLiveData<Boolean>()

    override fun onCreate() {
        super.onCreate()

        enableVideoButton.postValue(false)

        callManager.changeLocalStream = { videoStream, added ->
            localVideoRenderer.postValue { videoSink ->
                if (added) {
                    videoStream.addVideoRenderer(videoSink, RenderScaleType.SCALE_FIT)
                } else {
                    videoStream.removeVideoRenderer(videoSink)
                }
            }
        }

        callManager.changeRemoteStream = { videoStream, added ->
            remoteVideoRenderer.postValue { videoSink ->
                if (added)
                    videoStream.addVideoRenderer(videoSink, RenderScaleType.SCALE_FIT)
                else
                    videoStream.removeVideoRenderer(videoSink)
            }
            _receivingVideo = added
        }

        callManager.onCallConnect = {
            enableVideoButton.postValue(true)
        }

        callManager.onCallDisconnect = { failed, reason ->
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                moveToMainActivity.postValue(Unit)
            }
        }

        callManager.setRenderSurface = { surface, size ->
            deepARHelper.setRenderSurface(surface, size)
        }

        cameraHelper.onImageReceived = { image, mirroring ->
            deepARHelper.processImage(image, mirroring)
        }
    }

    fun onCreateWithCall(isIncoming: Boolean, isActive: Boolean) {
        if (isActive) {
            // On return to call from notification
            enableVideoButton.postValue(true)
            _muted = callManager.muted
            _sendingVideo = callManager.hasLocalVideoStream
            _receivingVideo = callManager.hasRemoteVideoStream
            if (_sendingVideo)
                localVideoRenderer.postValue { videoSink ->
                    callManager.localVideoStream?.addVideoRenderer(
                        videoSink,
                        RenderScaleType.SCALE_FIT,
                    )
                }
            if (_receivingVideo)
                remoteVideoRenderer.postValue { videoSink ->
                    callManager.remoteVideoStream?.addVideoRenderer(
                        videoSink,
                        RenderScaleType.SCALE_FIT,
                    )
                }
        } else {
            deepARHelper.startDeepAR()
            cameraHelper.startCamera(cameraPreset, lensReset = true)
            callManager.attachCustomSource(cameraPreset)
            if (isIncoming) {
                try {
                    callManager.answerCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            } else {
                try {
                    callManager.startCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            }
        }
    }


    fun mute() {
        try {
            callManager.muteActiveCall(!_muted)
            _muted = !_muted
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun selectAudioDevice(device: String) {
        if (!device.contains("(Current)")) {
            callManager.selectAudioDevice(device)
        }
    }

    fun sendVideo() {
        enableVideoButton.postValue(false)

        _sendingVideo = !_sendingVideo

        if (_sendingVideo) {
            deepARHelper.startDeepAR()
            cameraHelper.startCamera(cameraPreset)
            callManager.attachCustomSource(cameraPreset)
        } else {
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
            callManager.releaseCustomVideoSource()
        }

        callManager.sendVideo(_sendingVideo) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
                _sendingVideo = !_sendingVideo
            }
            enableVideoButton.postValue(true)
        }
    }

    fun hangup() {
        try {
            callManager.hangup()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finish.postValue(Unit)
        }
    }

}
