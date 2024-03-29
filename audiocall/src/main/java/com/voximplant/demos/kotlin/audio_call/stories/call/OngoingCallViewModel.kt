/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.CallState
import com.voximplant.demos.kotlin.utils.Shared.getResource
import com.voximplant.sdk.hardware.AudioDevice
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class OngoingCallViewModel : ViewModel() {

    private val _callState = MutableLiveData<CallState>()
    private val _callStatus = MediatorLiveData<String?>()
    val callStatus: LiveData<String?>
        get() = _callStatus
    val muted
        get() = audioCallManager.muted
    private val _onHold = MutableLiveData<Boolean>()
    val onHold
        get() = _onHold
    private val _enableButtons = MutableLiveData(false)
    val enableButtons: LiveData<Boolean>
        get() = _enableButtons
    private val _enableKeypad = MutableLiveData(false)
    val enableKeypad: LiveData<Boolean>
        get() = _enableKeypad
    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?>
        get() = _userName
    private val _displayName = MutableLiveData<String?>()
    val displayName: LiveData<String?>
        get() = _displayName
    val charDTMF = MutableLiveData<String>()
    val onHideKeypadPressed = MutableLiveData<Unit>()

    val availableAudioDevices: StateFlow<List<AudioDevice>> = audioCallManager.availableAudioDevices
    val selectedAudioDevice: StateFlow<AudioDevice> = audioCallManager.selectedAudioDevice

    val moveToCallFailed = MutableLiveData<String>()
    val finishActivity = MutableLiveData<Unit>()

    init {
        _callStatus.addSource(audioCallManager.callDuration) { value ->
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val formattedCallDuration: String = dateFormat.format(Date(value))
            _callStatus.postValue(formattedCallDuration)
        }

        _callStatus.addSource(audioCallManager.callState.asLiveData()) { callState ->
            _callStatus.postValue(callState.toString())
            _callState.postValue(callState)
            updateDisplayName()
            if (callState == CallState.CONNECTED) {
                _enableButtons.postValue(true)
                if (audioCallManager.onHold.value == true) {
                    _callStatus.postValue(getResource.getString(R.string.call_on_hold))
                    _enableKeypad.postValue(false)
                    onHideKeypadPressed.postValue(Unit)
                } else {
                    _enableKeypad.postValue(true)
                }
            } else {
                _enableButtons.postValue(false)
                onHideKeypadPressed.postValue(Unit)
            }
        }

        _callStatus.addSource(audioCallManager.onHold) { onHold ->
            _onHold.postValue(onHold)
            if (onHold) {
                _callStatus.postValue(getResource.getString(R.string.call_on_hold))
                onHideKeypadPressed.postValue(Unit)
            }
            if (_callState.value == CallState.CONNECTED) {
                _enableKeypad.postValue(!onHold)
            }
        }

        audioCallManager.onCallConnect =
            {
                _userName.postValue(audioCallManager.endpointUsername)
                _displayName.postValue(audioCallManager.endpointDisplayName)
                _enableButtons.postValue(true)
            }

        audioCallManager.onCallDisconnect =
            { failed, reason ->
                if (failed) {
                    moveToCallFailed.postValue(reason)
                } else {
                    finishActivity.postValue(Unit)
                }
            }

    }

    fun onCreateWithCall(
        isOngoing: Boolean,
        isOutgoing: Boolean,
        isIncoming: Boolean,
    ) {
        if (isOngoing || isIncoming) {
            // On return to call from notification

            _displayName.postValue(audioCallManager.endpointUsername)
            _displayName.postValue(audioCallManager.endpointDisplayName)
            _enableButtons.postValue(true)
        } else if (isOutgoing) {
            _userName.postValue(audioCallManager.endpointUsername)
            _displayName.postValue(audioCallManager.endpointDisplayName)
            try {
                audioCallManager.startOutgoingCall()
            } catch (e: CallManagerException) {
                Log.e(APP_TAG, "OngoingCallViewModel::onCreateWithCall ${e.message}")
                finishActivity.postValue(Unit)
            }
        } else {
            finishActivity.postValue(Unit)
        }
    }

    private fun updateDisplayName() {
        _displayName.postValue(audioCallManager.endpointDisplayName)
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
        audioCallManager.onHold.value?.let { isOnHold ->
            audioCallManager.holdOngoingCall(!isOnHold)
        }
    }

    fun selectAudioDevice(audioDevice: AudioDevice) {
        audioCallManager.selectAudioDevice(audioDevice)
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
        _callStatus.removeSource(audioCallManager.callState.asLiveData())
        _callStatus.removeSource(audioCallManager.callDuration)
        _callStatus.removeSource(audioCallManager.onHold)
    }

}
