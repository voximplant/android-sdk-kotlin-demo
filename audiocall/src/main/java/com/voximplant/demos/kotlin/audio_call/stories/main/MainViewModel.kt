/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.services.AuthServiceListener
import com.voximplant.demos.kotlin.utils.*

class MainViewModel : BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService

    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToLogin = MutableLiveData<Unit>()
    val invalidInputError = MutableLiveData<Int>()

    override fun onCreate() {
        super.onCreate()
        displayName.postValue("${Shared.getResource.getString(R.string.logged_in_as)} ${authService.displayName}")
    }

    fun call(user: String?) {
        when {
            user.isNullOrEmpty() -> invalidInputError.postValue(R.string.empty_field_warning)
            else -> {
                showProgress.postValue(R.string.reconnecting)
                authService.reconnectIfNeeded { error ->
                    hideProgress.postValue(Unit)
                    error?.let {
                        if (error == AuthError.NetworkIssues) {
                            postError(R.string.error_failed_to_reconnect_and_check_connectivity)
                        } else {
                            finish.postValue(Unit)
                        }
                    } ?: run {
                        try {
                            audioCallManager.createOutgoingCall(user)
                            moveToCall.postValue(Unit)
                        } catch (e: CallManagerException) {
                            Log.e(APP_TAG, "MainViewModel::call ${e.message}")
                            postError(e.message.toString())
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        authService.logout()
        moveToLogin.postValue(Unit)
    }
}