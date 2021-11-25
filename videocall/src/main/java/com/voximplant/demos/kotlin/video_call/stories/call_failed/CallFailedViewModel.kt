package com.voximplant.demos.kotlin.video_call.stories.call_failed

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.authService
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager

class CallFailedViewModel : BaseViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToMainActivity = MutableLiveData<Unit>()

    override fun onCreate() {
        super.onCreate()
        displayName.postValue(
            voximplantCallManager.latestCallerDisplayName
                ?: voximplantCallManager.latestCallerUsername.orEmpty()
        )
    }

    fun cancel() {
        moveToMainActivity.postValue(Unit)
    }

    fun callBack() {
        showProgress.postValue(R.string.reconnecting)
        authService.reconnectIfNeeded { error ->
            hideProgress.postValue(Unit)
            error?.let {
                if (error == AuthError.NetworkIssues) {
                    postError(R.string.error_failed_to_reconnect_and_check_connectivity)
                } else {
                    moveToMainActivity.postValue(Unit)
                }
            } ?: run {
                try {
                    voximplantCallManager.createCall(voximplantCallManager.latestCallerUsername.orEmpty())
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    postError(e.message.toString())
                }
            }
        }
    }

}
