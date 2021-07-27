package com.voximplant.demos.kotlin.video_call.stories.call

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.cameraManager
import com.voximplant.sdk.hardware.VideoQuality

class CallViewModel : BaseViewModel() {
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
        get() = Shared.voximplantCallManager.availableAudioDevices.map { device ->
            if (device.name == Shared.voximplantCallManager.selectedAudioDevice.value?.name) {
                "${device.name} (Current)"
            } else {
                device.name
            }
        }

    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()
    val enableVideoButton = MutableLiveData<Boolean>()
    val enableHoldButton = MutableLiveData<Boolean>()
    val enableSharingButton = MutableLiveData<Boolean>()

    override fun onCreate() {
        super.onCreate()

        enableVideoButton.postValue(false)
        enableHoldButton.postValue(false)

        Shared.voximplantCallManager.onCallConnect = {
            enableVideoButton.postValue(true)
            enableHoldButton.postValue(true)
        }

        Shared.voximplantCallManager.onCallDisconnect = { failed, reason ->
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                moveToMainActivity.postValue(Unit)
            }
        }

        Shared.voximplantCallManager.initVideoStreams()
    }

    fun onCreateWithCall(isIncoming: Boolean, isActive: Boolean) {
        if (isActive) {
            // On return to call from notification
            enableVideoButton.postValue(true)
            enableHoldButton.postValue(true)
            _muted = Shared.voximplantCallManager.muted
            _onHold = Shared.voximplantCallManager.onHold
            _sharingScreen = Shared.voximplantCallManager.sharingScreen
            _sendingVideo = Shared.voximplantCallManager.hasLocalVideoStream
        } else {
            if (isIncoming) {
                try {
                    Shared.voximplantCallManager.answerCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            } else {
                try {
                    Shared.voximplantCallManager.startCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            }
        }
    }

    fun mute() {
        try {
            Shared.voximplantCallManager.muteActiveCall(!_muted)
            _muted = !_muted
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun hold() {
        enableHoldButton.postValue(false)
        _onHold = !_onHold
        Shared.voximplantCallManager.holdActiveCall(_onHold) { error ->
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
            Shared.voximplantCallManager.selectAudioDevice(device)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun shareScreen(request: ((Intent?) -> Unit) -> Unit) {
        enableSharingButton.postValue(false)
        enableVideoButton.postValue(false)

        _sharingScreen = !_sharingScreen

        if (_sharingScreen) {
            request { intent ->
                intent?.let {
                    _sendingVideo = false
                    Shared.voximplantCallManager.shareScreen(it) { e ->
                        e?.let { error ->
                            Log.e(APP_TAG, error.message.toString())
                            postError(error.message.toString())
                            _sharingScreen = !_sharingScreen
                        }
                        enableSharingButton.postValue(true)
                        enableVideoButton.postValue(true)
                    }
                } ?: run {
                    _sharingScreen = false
                    enableSharingButton.postValue(true)
                    enableVideoButton.postValue(true)
                }
            }

        } else {
            Shared.voximplantCallManager.sendVideo(_sendingVideo) { error ->
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

        Shared.voximplantCallManager.sendVideo(_sendingVideo) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
                _sendingVideo = !_sendingVideo
            } ?: run {
                _sharingScreen = false
            }
            enableSharingButton.postValue(true)
            enableVideoButton.postValue(true)
        }
    }

    fun changeCam() {
        cameraType = if (cameraType == 1) {
            0
        } else {
            1
        }
        cameraManager.setCamera(cameraType, videoQuality)
    }

    fun hangup() {
        try {
            Shared.voximplantCallManager.hangup()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finish.postValue(Unit)
        }
    }

    companion object {
        private val videoQuality = VideoQuality.VIDEO_QUALITY_MEDIUM
        private var cameraType = 1
    }
}
