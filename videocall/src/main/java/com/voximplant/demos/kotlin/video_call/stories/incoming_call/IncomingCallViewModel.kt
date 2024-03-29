package com.voximplant.demos.kotlin.video_call.stories.incoming_call

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.BaseViewModel
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.Shared.notificationHelper
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager

class IncomingCallViewModel : BaseViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToCallFailed = MutableLiveData<String>()

    private val _localVideoPresetEnabled = MutableLiveData(true)
    val localVideoPresetEnabled: LiveData<Boolean>
        get() = _localVideoPresetEnabled

    init {
        voximplantCallManager.onCallDisconnect = { failed, reason ->
            finish.postValue(Unit)
            if (failed) {
                moveToCallFailed.postValue(reason)
            }
        }

        displayName.postValue(
            voximplantCallManager.endpointDisplayName
                ?: voximplantCallManager.endpointUsername.orEmpty()
        )
    }

    fun viewCreated() {
        // The call can be canceled before the fragment is created
        if (!voximplantCallManager.callExists) {
            Log.w(APP_TAG, "IncomingCallViewModel::checkCallExistence The call no longer exists")
            notificationHelper.cancelIncomingCallNotification()
            finish.postValue(Unit)
        }
    }

    fun answer() {
        moveToCall.postValue(Unit)
    }

    fun decline() {
        try {
            voximplantCallManager.declineCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        }
        finish.postValue(Unit)
    }

    fun toggleLocalVideoPreset() {
        _localVideoPresetEnabled.postValue(_localVideoPresetEnabled.value != true)
    }

}