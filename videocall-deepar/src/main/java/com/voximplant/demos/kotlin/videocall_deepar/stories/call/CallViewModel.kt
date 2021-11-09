/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
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
import com.voximplant.demos.kotlin.videocall_deepar.cameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.deepARHelper
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.ICustomVideoSourceListener
import org.webrtc.SurfaceTextureHelper
import java.text.SimpleDateFormat
import java.util.*

class CallViewModel : BaseViewModel() {
    private val _callState = MutableLiveData<CallState>()
    private val _callStatus = MediatorLiveData<String?>()
    val callStatus: LiveData<String?>
        get() = _callStatus
    val displayName = MutableLiveData<String>()
    val muted
        get() = Shared.voximplantCallManager.muted

    private val cameraPreset = CameraResolutionPreset.P640x480

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

    val showLocalVideoView: LiveData<Boolean>
        get() = Shared.voximplantCallManager.showLocalVideoView
    val showRemoteVideoView: LiveData<Boolean>
        get() = Shared.voximplantCallManager.showRemoteVideoView
    val remoteVideoIsPortrait: LiveData<Boolean>
        get() = Shared.voximplantCallManager.remoteVideoIsPortrait

    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()

    val activeDevice: LiveData<AudioDevice>
        get() = Shared.voximplantCallManager.selectedAudioDevice
    val enableVideoButton = MutableLiveData(false)

    init {
        _callStatus.addSource(Shared.voximplantCallManager.callState) { callState ->
            _callState.postValue(callState)
            _callStatus.postValue(callState.toString())
            when (callState) {
                CallState.CONNECTED, CallState.ON_HOLD -> {
                    enableVideoButton.postValue(true)
                }
                else -> {
                    enableVideoButton.postValue(false)
                }
            }
        }
        _callStatus.addSource(Shared.voximplantCallManager.callDuration) { value ->
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedCallDuration: String = dateFormat.format(Date(value))
            _callStatus.postValue(formattedCallDuration)
        }

        Shared.voximplantCallManager.onCallConnect = {
            displayName.postValue(Shared.voximplantCallManager.callerDisplayName)
            enableVideoButton.postValue(true)
        }

        Shared.voximplantCallManager.onCallDisconnect = { failed, reason ->
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
            Shared.voximplantCallManager.releaseCustomVideoSource()
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                moveToMainActivity.postValue(Unit)
            }
        }

        Shared.voximplantCallManager.setRenderSurface = { surface, size ->
            deepARHelper.setRenderSurface(surface, size)
        }

        cameraHelper.onImageReceived = { image, mirroring ->
            deepARHelper.processImage(image, mirroring)
        }

        Shared.voximplantCallManager.initVideoStreams()
    }

    fun onCreateWithCall(isIncoming: Boolean, isActive: Boolean) {
        if (isActive) {
            // On return to call from notification
            displayName.postValue(Shared.voximplantCallManager.callerDisplayName)

            _sendingVideo = Shared.voximplantCallManager.hasLocalVideoStream

            enableVideoButton.postValue(true)
        } else {
            deepARHelper.startDeepAR()
            cameraHelper.startCamera(cameraPreset, lensReset = true)
            attachCustomSource(cameraPreset)
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

    private fun attachCustomSource(cameraPreset: CameraResolutionPreset) {
        val surfaceTextureHelper: SurfaceTextureHelper? = SurfaceTextureHelper.create(
            DEEP_AR,
            Shared.eglBase.eglBaseContext,
        )
        surfaceTextureHelper?.setTextureSize(cameraPreset.height, cameraPreset.width)
        Shared.voximplantCallManager.customVideoSource = Voximplant.getCustomVideoSource()
        Shared.voximplantCallManager.customVideoSource?.setSurfaceTextureHelper(surfaceTextureHelper)
        Shared.voximplantCallManager.customVideoSource?.setCustomVideoSourceListener(object : ICustomVideoSourceListener {
            override fun onStarted() {
                if (surfaceTextureHelper?.surfaceTexture != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Shared.voximplantCallManager.setRenderSurface?.invoke(
                            Surface(surfaceTextureHelper.surfaceTexture),
                            Size(cameraPreset.height, cameraPreset.width)
                        )
                    }
                }
            }

            override fun onStopped() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Shared.voximplantCallManager.setRenderSurface?.invoke(null, Size(0, 0))
                }
            }
        })
        Shared.voximplantCallManager.managedCall?.useCustomVideoSource(Shared.voximplantCallManager.customVideoSource)
    }

    fun mute() {
        try {
            muted.value?.let { Shared.voximplantCallManager.muteActiveCall(!it) }
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun selectAudioDevice(id: Int) {
        Shared.voximplantCallManager.selectAudioDevice(id)
    }

    fun sendVideo() {
        enableVideoButton.postValue(false)

        if (!_sendingVideo) {
            deepARHelper.startDeepAR()
            cameraHelper.startCamera(cameraPreset)
        } else {
            deepARHelper.stopDeepAR()
            cameraHelper.stopCamera()
        }

        Shared.voximplantCallManager.sendVideo(!_sendingVideo) { error ->
            error?.let {
                Log.e(APP_TAG, it.message.toString())
                postError(it.message.toString())
            } ?: run {
                _sendingVideo = !_sendingVideo
            }
            enableVideoButton.postValue(true)
        }
    }

    fun hangup() {
        try {
            Shared.voximplantCallManager.hangup()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finish.postValue(Unit)
        }
    }

}
