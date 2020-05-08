package com.voximplant.demos.kotlin.video_call.services

import android.util.Log
import com.voximplant.demos.kotlin.video_call.utils.APP_TAG
import com.voximplant.demos.kotlin.video_call.utils.AuthError
import com.voximplant.sdk.client.LoginError

interface AuthServiceListener {
    fun onConnectionFailed(error: AuthError) {
        Log.e(APP_TAG, "Connection failed ${error.description}")
    }

    fun onConnectionClosed() {
        Log.i(APP_TAG, "Connection closed")
    }

    fun onLoginFailed(error: AuthError) {
        Log.e(APP_TAG, "Login failed ${error.description}")
    }

    fun onAlreadyLoggedIn(displayName: String) {
        Log.i(APP_TAG, "onAlreadyLoggedIn $displayName")
    }

    fun onLoginSuccess(displayName: String) {
        Log.i(APP_TAG, "Login success $displayName")
    }

    fun onLogout() {
        Log.i(APP_TAG, "Logout completed")
    }
}