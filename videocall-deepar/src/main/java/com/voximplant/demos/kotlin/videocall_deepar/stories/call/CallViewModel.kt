package com.voximplant.demos.kotlin.videocall_deepar.stories.call

import ai.deepar.ar.CameraResolutionPreset
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.videocall_deepar.cameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.deepARHelper
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.ICustomVideoSourceListener
import org.webrtc.SurfaceTextureHelper

class CallViewModel : BaseViewModel() {

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

    val enableVideoButton = MutableLiveData(false)

    override fun onCreate() {
        super.onCreate()

        Shared.voximplantCallManager.onCallConnect = {
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
            enableVideoButton.postValue(true)
            _muted = Shared.voximplantCallManager.muted
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
            Shared.voximplantCallManager.muteActiveCall(!_muted)
            _muted = !_muted
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            postError(e.message.toString())
        }
    }

    fun selectAudioDevice(device: String) {
        if (!device.contains("(Current)")) {
            Shared.voximplantCallManager.selectAudioDevice(device)
        }
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
