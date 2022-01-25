/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.AuthError
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.Shared.authService

class CallFailedViewModel : ViewModel() {
    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?>
        get() = _userName
    private val _displayName = MutableLiveData<String?>()
    val displayName: LiveData<String?>
        get() = _displayName

    val showProgress = MutableLiveData<Int>()
    val hideProgress = MutableLiveData<Unit>()
    val showIntSnackbar = MutableLiveData<Int>()
    val showStringSnackbar = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val finishActivity = MutableLiveData<Unit>()

    fun setEndpoint(userName: String?, displayName: String?) {
        Log.d(APP_TAG, "CallFailedViewModel::setEndpoint userName: $userName, displayName: $displayName")
        _userName.postValue(userName)
        _displayName.postValue(displayName)
    }

    fun cancel() {
        finishActivity.postValue(Unit)
    }

    fun callBack() {
        showProgress.postValue(R.string.reconnecting)
        authService.reconnectIfNeeded { error ->
            hideProgress.postValue(Unit)
            error?.let {
                if (error == AuthError.NetworkIssues) {
                    showIntSnackbar.postValue(R.string.error_failed_to_reconnect_and_check_connectivity)
                } else {
                    finishActivity.postValue(Unit)
                }
            } ?: run {
                try {
                    audioCallManager.createOutgoingCall(_userName.value.orEmpty())
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    showStringSnackbar.postValue(e.message.toString())
                }
            }
        }
    }

}
