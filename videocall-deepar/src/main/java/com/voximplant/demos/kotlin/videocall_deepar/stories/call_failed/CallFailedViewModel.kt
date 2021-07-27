package com.voximplant.demos.kotlin.videocall_deepar.stories.call_failed

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*

class CallFailedViewModel : BaseViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToMainActivity = MutableLiveData<Unit>()

    override fun onCreate() {
        super.onCreate()
        displayName.postValue(
            Shared.voximplantCallManager.latestCallerDisplayName
                ?: Shared.voximplantCallManager.latestCallerDisplayName.orEmpty()
        )
    }

    fun cancel() {
        moveToMainActivity.postValue(Unit)
    }

    fun callBack() {
        try {
            Shared.voximplantCallManager.createCall(Shared.voximplantCallManager.latestCallerUsername.orEmpty())
            moveToCall.postValue(Unit)
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            moveToMainActivity.postValue(Unit)
        }
    }
}