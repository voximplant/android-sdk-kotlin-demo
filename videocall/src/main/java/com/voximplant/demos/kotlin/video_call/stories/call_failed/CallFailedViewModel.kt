package com.voximplant.demos.kotlin.video_call.stories.call_failed

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.video_call.services.VoximplantCallManager
import com.voximplant.demos.kotlin.video_call.utils.APP_TAG
import com.voximplant.demos.kotlin.video_call.utils.BaseViewModel
import com.voximplant.demos.kotlin.video_call.utils.CallManagerException
import com.voximplant.demos.kotlin.video_call.utils.Shared

class CallFailedViewModel: BaseViewModel() {
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()

    override fun onCreate() {
        super.onCreate()
        displayName.postValue(callManager.latestCallDisplayName ?: callManager.latestCallUsername.orEmpty())
    }

    fun cancel() {
        finish.postValue(Unit)
    }

    fun callBack() {
        try {
            callManager.createCall(callManager.latestCallUsername.orEmpty())
            finish.postValue(Unit)
            moveToCall.postValue(Unit)
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finish.postValue(Unit)
        }
    }
}