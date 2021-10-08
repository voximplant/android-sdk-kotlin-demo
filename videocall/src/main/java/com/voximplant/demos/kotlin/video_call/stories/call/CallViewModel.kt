/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.call

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.BaseViewModel
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.Shared
import com.voximplant.demos.kotlin.utils.Shared.cameraManager
import com.voximplant.sdk.hardware.AudioDevice
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

    val activeDevice: LiveData<AudioDevice>
        get() = Shared.voximplantCallManager.selectedAudioDevice
    val enableHoldButton = MutableLiveData(false)
    val enableVideoButton = MutableLiveData(false)
    val enableSharingButton = MutableLiveData(false)

    override fun onCreate() {
        super.onCreate()

        Shared.voximplantCallManager.onCallConnect = {
            enableHoldButton.postValue(true)
            enableVideoButton.postValue(true)
            enableSharingButton.postValue(true)
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
            _muted = Shared.voximplantCallManager.muted
            _onHold = Shared.voximplantCallManager.onHold
            _sharingScreen = Shared.voximplantCallManager.sharingScreen
            _sendingVideo = Shared.voximplantCallManager.hasLocalVideoStream

            enableHoldButton.postValue(true)
            if (!_onHold) {
                enableVideoButton.postValue(true)
                enableSharingButton.postValue(true)
            }
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
        enableVideoButton.postValue(false)
        enableSharingButton.postValue(false)

        Shared.voximplantCallManager.holdActiveCall(!_onHold) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
            } ?: run {
                _onHold = !_onHold
            }
            enableHoldButton.postValue(true)
            if (!_onHold) {
                enableVideoButton.postValue(true)
                enableSharingButton.postValue(true)
            }
        }
    }

    fun selectAudioDevice(id: Int) {
        Shared.voximplantCallManager.selectAudioDevice(id)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun shareScreen(request: ((Intent?) -> Unit) -> Unit) {
        enableHoldButton.postValue(false)
        enableVideoButton.postValue(false)
        enableSharingButton.postValue(false)

        if (!_sharingScreen) {
            request { intent ->
                intent?.let {
                    Shared.voximplantCallManager.shareScreen(it) { e ->
                        e?.let { error ->
                            Log.e(APP_TAG, error.message.toString())
                            postError(error.message.toString())
                        } ?: run {
                            _sharingScreen = !_sharingScreen
                            _sendingVideo = false
                        }
                        enableHoldButton.postValue(true)
                        enableVideoButton.postValue(true)
                        enableSharingButton.postValue(true)
                    }
                } ?: run {
                    enableHoldButton.postValue(true)
                    enableVideoButton.postValue(true)
                    enableSharingButton.postValue(true)
                }
            }
        } else {
            Shared.voximplantCallManager.sendVideo(_sendingVideo) { error ->
                error?.let {
                    Log.e(APP_TAG, it.message.toString())
                    postError(it.message.toString())
                } ?: run {
                    _sharingScreen = !_sharingScreen
                }
                enableHoldButton.postValue(true)
                enableVideoButton.postValue(true)
                enableSharingButton.postValue(true)
            }
        }
    }

    fun sendVideo() {
        enableHoldButton.postValue(false)
        enableVideoButton.postValue(false)
        enableSharingButton.postValue(false)

        Shared.voximplantCallManager.sendVideo(!_sendingVideo) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
            } ?: run {
                _sendingVideo = !_sendingVideo
                _sharingScreen = false
            }
            enableHoldButton.postValue(true)
            enableVideoButton.postValue(true)
            enableSharingButton.postValue(true)
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
