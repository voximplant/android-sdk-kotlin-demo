/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.call

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.BaseViewModel
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.CallState
import com.voximplant.demos.kotlin.utils.Shared.cameraManager
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.VideoQuality
import java.text.SimpleDateFormat
import java.util.*

class CallViewModel : BaseViewModel() {
    private val _callState = MutableLiveData<CallState>()
    private val _callStatus = MediatorLiveData<String?>()
    val callStatus: LiveData<String?>
        get() = _callStatus
    val displayName = MutableLiveData<String>()
    val muted
        get() = voximplantCallManager.muted
    val onHold
        get() = voximplantCallManager.onHold

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
        get() = voximplantCallManager.availableAudioDevices.map { device ->
            if (device.name == voximplantCallManager.selectedAudioDevice.value?.name) {
                "${device.name} (Current)"
            } else {
                device.name
            }
        }

    val showLocalVideoView: LiveData<Boolean>
        get() = voximplantCallManager.showLocalVideoView
    val showRemoteVideoView: LiveData<Boolean>
        get() = voximplantCallManager.showRemoteVideoView
    val remoteVideoIsPortrait: LiveData<Boolean>
        get() = voximplantCallManager.remoteVideoIsPortrait

    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()

    val activeDevice: LiveData<AudioDevice>
        get() = voximplantCallManager.selectedAudioDevice
    val enableHoldButton = MutableLiveData(false)
    val enableVideoButton = MutableLiveData(false)
    val enableSharingButton = MutableLiveData(false)

    init {
        _callStatus.addSource(voximplantCallManager.callState) { callState ->
            _callState.postValue(callState)
            _callStatus.postValue(callState.toString())
            when (callState) {
                CallState.CONNECTED, CallState.ON_HOLD -> {
                    enableHoldButton.postValue(true)
                    enableVideoButton.postValue(true)
                    enableSharingButton.postValue(true)
                }
                else -> {
                    enableHoldButton.postValue(false)
                    enableVideoButton.postValue(false)
                    enableSharingButton.postValue(false)
                }
            }
        }

        _callStatus.addSource(voximplantCallManager.callDuration) { value ->
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedCallDuration: String = dateFormat.format(Date(value))
            _callStatus.postValue(formattedCallDuration)
        }

        voximplantCallManager.onCallConnect = {
            displayName.postValue(voximplantCallManager.callerDisplayName)
            enableHoldButton.postValue(true)
            enableVideoButton.postValue(true)
            enableSharingButton.postValue(true)
        }

        voximplantCallManager.onCallDisconnect = { failed, reason ->
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                moveToMainActivity.postValue(Unit)
            }
        }

        voximplantCallManager.initVideoStreams()
    }

    fun onCreateWithCall(isIncoming: Boolean, isActive: Boolean) {
        if (isActive || isIncoming) {
            // On return to call from notification
            displayName.postValue(voximplantCallManager.callerDisplayName)

            _sharingScreen = voximplantCallManager.sharingScreen
            _sendingVideo = voximplantCallManager.hasLocalVideoStream

            enableHoldButton.postValue(true)
        } else {
            try {
                voximplantCallManager.startCall()
            } catch (e: CallManagerException) {
                Log.e(APP_TAG, e.message.toString())
                finish.postValue(Unit)
            }
        }
    }

    fun mute() {
        try {
            muted.value?.let { voximplantCallManager.muteActiveCall(!it) }
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun hold() {
        onHold.value?.let { isOnHold ->
            voximplantCallManager.holdActiveCall(!isOnHold)
        }
    }

    fun selectAudioDevice(id: Int) {
        voximplantCallManager.selectAudioDevice(id)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun shareScreen(request: ((Intent?) -> Unit) -> Unit) {
        enableHoldButton.postValue(false)
        enableVideoButton.postValue(false)
        enableSharingButton.postValue(false)

        if (!_sharingScreen) {
            request { intent ->
                intent?.let {
                    voximplantCallManager.shareScreen(it) { e ->
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
            voximplantCallManager.sendVideo(_sendingVideo) { error ->
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

        voximplantCallManager.sendVideo(!_sendingVideo) { error ->
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
            voximplantCallManager.hangup()
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
