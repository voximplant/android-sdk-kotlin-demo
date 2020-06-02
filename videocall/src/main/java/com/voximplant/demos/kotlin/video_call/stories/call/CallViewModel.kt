package com.voximplant.demos.kotlin.video_call.stories.call

import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.video_call.services.VoximplantCallManager
import com.voximplant.demos.kotlin.video_call.utils.APP_TAG
import com.voximplant.demos.kotlin.video_call.utils.BaseViewModel
import com.voximplant.demos.kotlin.video_call.utils.CallManagerException
import com.voximplant.demos.kotlin.video_call.utils.Shared
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.VideoQuality
import org.webrtc.VideoSink

class CallViewModel: BaseViewModel() {
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager

    val onHold = MutableLiveData<Boolean>()
    private var _onHold: Boolean = false
        set(value) {
            field = value
            onHold.postValue(value)
        }

    val muted = MutableLiveData<Boolean>()
    private var _muted: Boolean = false
        set(value) {
            field = value
            muted.postValue(value)
        }

    val sharingScreen = MutableLiveData<Boolean>()
    private var _sharingScreen: Boolean = false
        set(value) {
            field = value
            sharingScreen.postValue(value)
        }

    val sendingVideo = MutableLiveData<Boolean>()
    private var _sendingVideo: Boolean = true
        set(value) {
            field = value
            sendingVideo.postValue(value)
        }

    val availableAudioDevices: List<String>
        get() = callManager.availableAudioDevices.map { device ->
            if (device.name == callManager.selectedAudioDevice.name) {
                "${device.name} (Current)"
            } else {
                device.name
            }
        }

    val localVideoStreamAdded = MutableLiveData<(VideoSink) -> Unit>()
    val remoteVideoStreamAdded = MutableLiveData<(VideoSink) -> Unit>()
    val localVideoStreamRemoved = MutableLiveData<Unit>()
    val remoteVideoStreamRemoved = MutableLiveData<Unit>()
    val moveToCallFailed = MutableLiveData<String>()
    val enableVideoButton = MutableLiveData<Boolean>()
    val enableHoldButton = MutableLiveData<Boolean>()
    val enableSharingButton = MutableLiveData<Boolean>()

    override fun onCreate() {
        super.onCreate()

        enableVideoButton.postValue(false)
        enableHoldButton.postValue(false)

        callManager.videoStreamAdded = { local, completion ->
            if (local) {
                localVideoStreamAdded.postValue(completion)
            } else {
                remoteVideoStreamAdded.postValue(completion)
            }
        }

        callManager.videoStreamRemoved = { local ->
            if (local) {
                localVideoStreamRemoved.postValue(Unit)
            } else {
                remoteVideoStreamRemoved.postValue(Unit)
            }
        }

        callManager.onCallConnect = {
            enableVideoButton.postValue(true)
            enableHoldButton.postValue(true)
        }

        callManager.onCallDisconnect = { failed, reason ->
            finish.postValue(Unit)
            if (failed) {
                moveToCallFailed.postValue(reason)
            }
        }
    }

    fun onCreateWithCall(isIncoming: Boolean) {
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

    fun mute() {
        try {
            callManager.muteActiveCall(!_muted)
            _muted = !_muted
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun hold() {
        enableHoldButton.postValue(false)
        _onHold = !_onHold
        callManager.holdActiveCall(_onHold) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
                _onHold = !_onHold
            }
            enableHoldButton.postValue(true)
        }
    }

    fun selectAudioDevice(device: String) {
        if (!device.contains("(Current)")) {
            callManager.selectAudioDevice(device)
        }
    }

    fun shareScreen(request: ((Intent?) -> Unit) -> Unit) {
        enableSharingButton.postValue(false)
        enableVideoButton.postValue(false)

        _sharingScreen = !_sharingScreen

        if (_sharingScreen) {
            request { intent ->
                intent?.let {
                    callManager.shareScreen(it) { e ->
                        e?.let { error ->
                            Log.e(APP_TAG, error.message.toString())
                            postError(error.message.toString())
                            _sharingScreen = !_sharingScreen
                        }
                        enableSharingButton.postValue(true)
                        enableVideoButton.postValue(true)
                    }
                } ?: {
                    _sharingScreen = false
                    enableSharingButton.postValue(true)
                    enableVideoButton.postValue(true)
                }()
            }

        } else {
            callManager.sendVideo(_sendingVideo) { error ->
                error?.let {
                    Log.e(APP_TAG, it.message.toString())
                    postError(it.message.toString())
                    _sharingScreen = !_sharingScreen
                }
                enableSharingButton.postValue(true)
                enableVideoButton.postValue(true)
            }
        }
    }

    fun sendVideo() {
        enableSharingButton.postValue(false)
        enableVideoButton.postValue(false)

        _sendingVideo = !_sendingVideo

        callManager.sendVideo(_sendingVideo) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
                _sendingVideo = !_sendingVideo
            } ?: {
                _sharingScreen = false
            }()
            enableSharingButton.postValue(true)
            enableVideoButton.postValue(true)
        }
    }

    fun changeCam() {
        cameraType = if (cameraType == 1) { 0 } else { 1 }
        Shared.cameraManager.setCamera(cameraType, videoQuality)
    }

    fun hangup() {
        try {
            callManager.hangup()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        } finally {
            finish.postValue(Unit)
        }
    }

    companion object {
        private val videoQuality = VideoQuality.VIDEO_QUALITY_MEDIUM
        private var cameraType = 1
    }
}