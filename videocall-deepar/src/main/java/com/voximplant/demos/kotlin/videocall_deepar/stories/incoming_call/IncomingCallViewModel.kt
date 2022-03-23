package com.voximplant.demos.kotlin.videocall_deepar.stories.incoming_call

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.BaseViewModel
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager

class IncomingCallViewModel : BaseViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()

    val localVideoPresetEnabled: LiveData<Boolean>
        get() = voximplantCallManager.localVideoPresetEnabled

    override fun onCreate() {
        super.onCreate()

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

    fun answer() {
        moveToCall.postValue(Unit)
    }

    fun decline() {
        try {
            voximplantCallManager.declineCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        }
        moveToMainActivity.postValue(Unit)
    }

    fun switchPresetCamera() {
        voximplantCallManager.switchPresetCamera()
    }

}