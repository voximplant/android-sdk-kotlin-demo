package com.voximplant.demos.kotlin.videocall_deepar.stories.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.services.AuthServiceListener
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager

class MainViewModel : BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService

    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToLogin = MutableLiveData<Unit>()

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
                    voximplantCallManager.createCall(user ?: "", sendVideo = _localVideoPresetEnabled.value == true)
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    postError(e.message.toString())
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
