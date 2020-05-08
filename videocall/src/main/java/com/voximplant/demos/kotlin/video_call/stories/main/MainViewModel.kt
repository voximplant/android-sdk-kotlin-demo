package com.voximplant.demos.kotlin.video_call.stories.main

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.services.AuthService
import com.voximplant.demos.kotlin.video_call.services.AuthServiceListener
import com.voximplant.demos.kotlin.video_call.services.VoximplantCallManager
import com.voximplant.demos.kotlin.video_call.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.client.ClientState
import com.voximplant.sdk.client.IClient

class MainViewModel: BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService
    private val callManager: VoximplantCallManager = Shared.voximplantCallManager

    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()

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
            } ?: {
                try {
                    callManager.createCall(user ?: "")
                    moveToCall.postValue(Unit)
                } catch (e: CallManagerException) {
                    Log.e(APP_TAG, e.message.toString())
                    postError(e.message.toString())
                }
            }()
        }
    }

    fun logout() {
        authService.logout()
        finish.postValue(Unit)
    }
}