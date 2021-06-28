package com.voximplant.demos.kotlin.videocall_deepar.stories.call_failed

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.videocall_deepar.services.VoximplantCallManager
import com.voximplant.demos.kotlin.videocall_deepar.utils.APP_TAG
import com.voximplant.demos.kotlin.videocall_deepar.utils.BaseViewModel
import com.voximplant.demos.kotlin.videocall_deepar.utils.CallManagerException
import com.voximplant.demos.kotlin.videocall_deepar.utils.Shared

class CallFailedViewModel: BaseViewModel() {
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToMainActivity = MutableLiveData<Unit>()

    override fun onCreate() {
        super.onCreate()
        displayName.postValue(callManager.latestCallDisplayName ?: callManager.latestCallUsername.orEmpty())
    }

    fun cancel() {
        moveToMainActivity.postValue(Unit)
    }

    fun callBack() {
        try {
            callManager.createCall(callManager.latestCallUsername.orEmpty())
            moveToCall.postValue(Unit)
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            moveToMainActivity.postValue(Unit)
        }
    }
}