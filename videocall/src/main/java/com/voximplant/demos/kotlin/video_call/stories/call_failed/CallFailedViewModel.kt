package com.voximplant.demos.kotlin.video_call.stories.call_failed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.authService
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager

class CallFailedViewModel : BaseViewModel() {
    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?>
        get() = _userName
    private val _displayName = MutableLiveData<String?>()
    val displayName: LiveData<String?>
        get() = _displayName

    val moveToCall = MutableLiveData<Unit>()
    val moveToMainActivity = MutableLiveData<Unit>()

    fun setEndpoint(userName: String?, displayName: String?) {
        Log.d(APP_TAG, "CallFailedViewModel::setEndpoint userName: $userName, displayName: $displayName")
        _userName.postValue(userName)
        _displayName.postValue(displayName)
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
                    voximplantCallManager.createCall(_userName.value.orEmpty())
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    postError(e.message.toString())
                }
            }
        }
    }

}
