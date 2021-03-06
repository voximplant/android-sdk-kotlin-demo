package com.voximplant.demos.kotlin.videocall_deepar.stories.incoming_call

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.videocall_deepar.services.VoximplantCallManager
import com.voximplant.demos.kotlin.videocall_deepar.utils.APP_TAG
import com.voximplant.demos.kotlin.videocall_deepar.utils.BaseViewModel
import com.voximplant.demos.kotlin.videocall_deepar.utils.CallManagerException
import com.voximplant.demos.kotlin.videocall_deepar.utils.Shared

class IncomingCallViewModel: BaseViewModel() {
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()


    override fun onCreate() {
        super.onCreate()

        callManager.onCallDisconnect = { failed, reason ->
            finish.postValue(Unit)
            if (failed) {
                moveToCallFailed.postValue(reason)
            }
        }

        displayName.postValue(callManager.latestCallDisplayName ?: callManager.latestCallUsername.orEmpty())
    }

    fun answer() {
        moveToCall.postValue(Unit)
    }

    fun decline() {
        try {
            callManager.declineCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        }
        moveToMainActivity.postValue(Unit)
    }
}