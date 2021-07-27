package com.voximplant.demos.kotlin.videocall_deepar.stories.incoming_call

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*

class IncomingCallViewModel : BaseViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToCallFailed = MutableLiveData<String>()
    val moveToMainActivity = MutableLiveData<Unit>()


    override fun onCreate() {
        super.onCreate()

        Shared.voximplantCallManager.onCallDisconnect = { failed, reason ->
            finish.postValue(Unit)
            if (failed) {
                moveToCallFailed.postValue(reason)
            }
        }

        displayName.postValue(
            Shared.voximplantCallManager.latestCallerDisplayName
                ?: Shared.voximplantCallManager.latestCallerUsername.orEmpty()
        )
    }

    fun answer() {
        moveToCall.postValue(Unit)
    }

    fun decline() {
        try {
            Shared.voximplantCallManager.declineCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        }
        moveToMainActivity.postValue(Unit)
    }
}