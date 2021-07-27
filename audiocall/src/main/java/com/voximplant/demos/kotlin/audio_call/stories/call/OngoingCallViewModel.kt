/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.R
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.sdk.hardware.IAudioDeviceEventsListener
import com.voximplant.sdk.hardware.IAudioDeviceManager

class OngoingCallViewModel : ViewModel(), IAudioDeviceEventsListener {

    private var audioDeviceManager: IAudioDeviceManager = Voximplant.getAudioDeviceManager()
    val availableAudioDevices: List<String>
        get() = audioDeviceManager.audioDevices.map { audioDevice ->
            if (audioDevice.equals(audioDeviceManager.activeDevice)) {
                "${audioDevice.name} (Current)"
            } else {
                audioDevice.name
            }
        }

    val muted = MutableLiveData(false)
    private var _muted: Boolean = false
        set(value) {
            field = value
            muted.postValue(value)
        }

    val moveToCallFailed = MutableLiveData<String>()
    val finishActivity = MutableLiveData<Unit>()

    val enableButtons = MutableLiveData(false)
    val displayName = MutableLiveData<String>()
    val callStatus = MutableLiveData(R.string.connecting)
    val charDTMF = MutableLiveData<String>()
    val onHideKeypadPressed = MutableLiveData<Unit>()
    val activeDevice = MutableLiveData(audioDeviceManager.activeDevice)

    init {
        audioCallManager.onCallConnect = {
            displayName.postValue(audioCallManager.callerDisplayName)
            callStatus.postValue(R.string.call_in_progress)
            enableButtons.postValue(true)
        }

        audioCallManager.onCallDisconnect = { failed, reason ->
            audioDeviceManager.removeAudioDeviceEventsListener(this)
            if (failed) {
                moveToCallFailed.postValue(reason)
            } else {
                finishActivity.postValue(Unit)
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
            _muted = audioCallManager.muted

            displayName.postValue(audioCallManager.callerDisplayName)
            callStatus.postValue(R.string.call_in_progress)
            enableButtons.postValue(true)
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
            audioCallManager.muteOngoingCall(!_muted)
            _muted = !_muted
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, "OngoingCallViewModel::mute ${e.message}")
        }
    }

    fun hold() {
        audioCallManager.onHold.value?.let { isOnHold ->
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
