/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.*
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.CallState
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.IAudioDeviceEventsListener
import com.voximplant.sdk.hardware.IAudioDeviceManager
import java.text.SimpleDateFormat
import java.util.*

class OngoingCallViewModel : ViewModel(), IAudioDeviceEventsListener {

    private val _callStatus = MediatorLiveData<String?>()
    val callStatus: LiveData<String?>
        get() = _callStatus
    val muted
        get() = audioCallManager.muted
    val onHold
        get() = audioCallManager.onHold
    private val _enableButtons = MutableLiveData(false)
    val enableButtons: LiveData<Boolean>
        get() = _enableButtons
    val displayName = MutableLiveData<String>()
    val charDTMF = MutableLiveData<String>()
    val onHideKeypadPressed = MutableLiveData<Unit>()
    private var audioDeviceManager: IAudioDeviceManager = Voximplant.getAudioDeviceManager()
    val availableAudioDevices: List<String>
        get() = audioDeviceManager.audioDevices.map { audioDevice ->
            if (audioDevice.equals(audioDeviceManager.activeDevice)) {
                "${audioDevice.name} (Current)"
            } else {
                audioDevice.name
            }
        }
    val activeDevice = MutableLiveData(audioDeviceManager.activeDevice)

    val moveToCallFailed = MutableLiveData<String>()
    val finishActivity = MutableLiveData<Unit>()

    init {
        _callStatus.addSource(audioCallManager.callState) { callState ->
            _callStatus.postValue(callState.toString())
        }

        _callStatus.addSource(audioCallManager.callDuration) { value ->
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedCallDuration: String = dateFormat.format(Date(value))
            _callStatus.postValue(formattedCallDuration)
        }

        audioCallManager.onCallConnect = {
            displayName.postValue(audioCallManager.callerDisplayName)
            _enableButtons.postValue(true)
        }

        audioCallManager.onCallDisconnect = { failed, reason ->
            audioDeviceManager.removeAudioDeviceEventsListener(this)
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                finishActivity.postValue(Unit)
            }
        }

        audioCallManager.callState.observeForever { callState ->
            when (callState) {
                CallState.CONNECTED, CallState.ON_HOLD -> {
                    _enableButtons.postValue(true)
                }
                else -> {
                    _enableButtons.postValue(false)
                    onHideKeypadPressed.postValue(Unit)
                }
            }
        }

        audioDeviceManager.addAudioDeviceEventsListener(this)
    }

    fun onCreateWithCall(
        isOngoing: Boolean,
        isOutgoing: Boolean,
        isIncoming: Boolean,
    ) {
        if (isOngoing || isIncoming) {
            // On return to call from notification

            displayName.postValue(audioCallManager.callerDisplayName)
            _enableButtons.postValue(true)
        } else {
            if (isOutgoing) {
                try {
                    audioCallManager.startOutgoingCall()
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, "OngoingCallViewModel::onCreateWithCall ${e.message}")
                    finishActivity.postValue(Unit)
                }
            }
        }
    }

    private fun updateDisplayName() {
        displayName.postValue(audioCallManager.callerDisplayName)
    }

    fun onHideKeypadPressed() {
        updateDisplayName()
        onHideKeypadPressed.postValue(Unit)
    }

    fun mute() {
        try {
            muted.value?.let { audioCallManager.muteOngoingCall(!it) }
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, "OngoingCallViewModel::mute ${e.message}")
        }
    }

    fun hold() {
        onHold.value?.let { isOnHold ->
            audioCallManager.holdOngoingCall(!isOnHold)
        }
    }

    fun selectAudioDevice(id: Int) {
        audioDeviceManager.selectAudioDevice(audioDeviceManager.audioDevices[id])
    }

    fun sendDTMF(DTMF: String) {
        charDTMF.postValue(DTMF)
        audioCallManager.sendDTMF(DTMF)
    }

    fun hangup() {
        try {
            audioCallManager.hangupOngoingCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, "OngoingCallViewModel::hangup ${e.message}")
            finishActivity.postValue(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioDeviceManager.removeAudioDeviceEventsListener(this)
    }

    override fun onAudioDeviceChanged(currentAudioDevice: AudioDevice?) {
        activeDevice.postValue(currentAudioDevice)
    }

    override fun onAudioDeviceListChanged(newDeviceList: MutableList<AudioDevice>?) {}
}
