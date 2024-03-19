/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.services.AuthServiceListener
import com.voximplant.demos.kotlin.services.VoximplantCallManager
import com.voximplant.demos.kotlin.utils.*

class MainViewModel : BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager

    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToLogin = MutableLiveData<Unit>()
    val callToFieldError = MutableLiveData<Int>()

    private val _localVideoPresetEnabled = MutableLiveData(true)
    val localVideoPresetEnabled: LiveData<Boolean>
        get() = _localVideoPresetEnabled

    init {
        authService.listener = this
    }

    override fun onCreate() {
        super.onCreate()
        displayName.postValue("Logged in as ${authService.displayName}")
    }

    fun call(user: String?) {
        when {
            user.isNullOrEmpty() -> callToFieldError.postValue(R.string.required_field)
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
                            callManager.createCall(user, sendVideo = _localVideoPresetEnabled.value == true)
                            moveToCall.postValue(Unit)
                        } catch (e: CallManagerException) {
                            Log.e(APP_TAG, e.message.toString())
                            postError(e.message.toString())
                        }
                    }
                }
            }
        }
    }

    fun toggleLocalVideoPreset() {
        _localVideoPresetEnabled.postValue(_localVideoPresetEnabled.value != true)
    }

    fun logout() {
        authService.logout()
    }

    override fun onConnectionFailed(error: AuthError) {
        postError(R.string.error_logout_failed_network_issues)
    }

    override fun onLogout() {
        moveToLogin.postValue(Unit)
    }

}
