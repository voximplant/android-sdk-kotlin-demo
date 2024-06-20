/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.videocall_deepar.stories.call

import ai.deepar.ar.CameraResolutionPreset
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.eglBase
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager
import com.voximplant.demos.kotlin.videocall_deepar.cameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.deepARHelper
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.ICustomVideoSourceListener
import com.voximplant.webrtc.SurfaceTextureHelper
import java.text.SimpleDateFormat
import java.util.*

class CallViewModel : BaseViewModel() {
    private val _callState = MutableLiveData<CallState>()
    private val _callStatus = MediatorLiveData<String?>()
    val callStatus: LiveData<String?>
        get() = _callStatus
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String>
        get() = _userName
    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String>
        get() = _displayName
    val muted
        get() = voximplantCallManager.muted

    private val cameraPreset = CameraResolutionPreset.P640x480

    val sendingLocalVideo: LiveData<Boolean>
        get() = voximplantCallManager.sendingLocalVideo

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
    val enableVideoButton = MutableLiveData(false)

    init {
        _callStatus.addSource(voximplantCallManager.callState) { callState ->
            _callState.postValue(callState)
            _callStatus.postValue(callState.toString())
            if (callState == CallState.CONNECTED) {
                enableVideoButton.postValue(true)
            } else {
                enableVideoButton.postValue(false)
            }
        }
        _callStatus.addSource(voximplantCallManager.callDuration) { value ->
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedCallDuration: String = dateFormat.format(Date(value))
            _callStatus.postValue(formattedCallDuration)
        }
        voximplantCallManager.onCallConnect = {
            _userName.postValue(voximplantCallManager.endpointUsername)
            _displayName.postValue(voximplantCallManager.endpointDisplayName)
            enableVideoButton.postValue(true)
        }

        voximplantCallManager.onCallDisconnect = { failed, reason ->
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
            voximplantCallManager.releaseCustomVideoSource()
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                moveToMainActivity.postValue(Unit)
            }
        }

        voximplantCallManager.setRenderSurface = { surface, size ->
            deepARHelper.setRenderSurface(surface, size)
        }

        cameraHelper.onImageReceived = { image, mirroring ->
            deepARHelper.processImage(image, mirroring)
        }

        voximplantCallManager.initVideoStreams()
    }

    fun onCreateWithCall(isIncoming: Boolean, isActive: Boolean, sendVideo: Boolean = true) {
        if (isActive) {
            // On return to call from notification
            _userName.postValue(voximplantCallManager.endpointUsername)
            _displayName.postValue(voximplantCallManager.endpointDisplayName)

            enableVideoButton.postValue(true)
        } else {
            if (sendVideo) {
                deepARHelper.startDeepAR()
                cameraHelper.startCamera(cameraPreset, lensReset = true)
            }
            attachCustomSource(cameraPreset)
            if (isIncoming) {
                try {
                    voximplantCallManager.answerCall(sendVideo = sendVideo)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            } else {
                _userName.postValue(voximplantCallManager.endpointUsername)
                _displayName.postValue(voximplantCallManager.endpointDisplayName)
                try {
                    voximplantCallManager.startCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    finish.postValue(Unit)
                }
            }
        }
    }

    private fun attachCustomSource(cameraPreset: CameraResolutionPreset) {
        val surfaceTextureHelper: SurfaceTextureHelper? = SurfaceTextureHelper.create(
            DEEP_AR,
            eglBase.eglBaseContext,
        )
        surfaceTextureHelper?.setTextureSize(cameraPreset.height, cameraPreset.width)
        voximplantCallManager.customVideoSource = Voximplant.getCustomVideoSource()
        voximplantCallManager.customVideoSource?.setSurfaceTextureHelper(surfaceTextureHelper)
        voximplantCallManager.customVideoSource?.setCustomVideoSourceListener(object : ICustomVideoSourceListener {
            override fun onStarted() {
                if (surfaceTextureHelper?.surfaceTexture != null) {
                    voximplantCallManager.setRenderSurface?.invoke(
                        Surface(surfaceTextureHelper.surfaceTexture),
                        Size(cameraPreset.height, cameraPreset.width)
                    )
                }
            }

            override fun onStopped() {
                voximplantCallManager.setRenderSurface?.invoke(null, Size(0, 0))
            }
        })
        voximplantCallManager.managedCall?.useCustomVideoSource(voximplantCallManager.customVideoSource)
    }

    fun mute() {
        try {
            muted.value?.let { voximplantCallManager.muteActiveCall(!it) }
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun selectAudioDevice(id: Int) {
        voximplantCallManager.selectAudioDevice(id)
    }

    fun sendVideo() {
        enableVideoButton.postValue(false)

        if (sendingLocalVideo.value != true) {
            deepARHelper.startDeepAR()
            cameraHelper.startCamera(cameraPreset)
        } else {
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
        }

        voximplantCallManager.sendVideo(sendingLocalVideo.value != true) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
            }
            enableVideoButton.postValue(true)
        }
    }

    fun hangup() {
        try {
            voximplantCallManager.hangup()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finish.postValue(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _callStatus.removeSource(voximplantCallManager.callState)
        _callStatus.removeSource(voximplantCallManager.callDuration)
        _callStatus.removeSource(voximplantCallManager.onHold)
    }

}
