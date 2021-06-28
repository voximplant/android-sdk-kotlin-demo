package com.voximplant.demos.kotlin.videocall_deepar.stories.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.videocall_deepar.R
import com.voximplant.demos.kotlin.videocall_deepar.services.AuthService
import com.voximplant.demos.kotlin.videocall_deepar.services.AuthServiceListener
import com.voximplant.demos.kotlin.videocall_deepar.services.VoximplantCallManager
import com.voximplant.demos.kotlin.videocall_deepar.utils.*

class MainViewModel: BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager

    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val moveToLogin = MutableLiveData<Unit>()

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
                    postError("Failed to reconnect, check the connection and try again")
                } else {
                    finish.postValue(Unit)
                }
            } ?: run {
                try {
                    callManager.createCall(user ?: "")
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    postError(e.message.toString())
                }
            }
        }
    }

    fun logout() {
        authService.logout()
        moveToLogin.postValue(Unit)
    }
}