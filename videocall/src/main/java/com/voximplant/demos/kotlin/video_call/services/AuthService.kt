package com.voximplant.demos.kotlin.video_call.services

import android.content.Context
import android.util.Log
import com.google.firebase.iid.*
import com.google.firebase.messaging.FirebaseMessaging
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.video_call.utils.*
import com.voximplant.sdk.client.*

class AuthService(
    private val client: IClient,
    private val tokens: Tokens,
    private val appContext: Context
) : IClientSessionListener, IClientLoginListener {
    private val clientState: ClientState
        get() = client.clientState
    var listener: AuthServiceListener? = null
    private var username: String?
        get() = USERNAME.getStringFromPrefs(appContext)
        set(newValue) =
            if (newValue == null) {
                USERNAME.removeKeyFromPrefs(appContext)
            } else {
                newValue.saveToPrefs(appContext, USERNAME)
            }
    private var password: String? = null
    private var firebaseToken: String? = null
    var displayName: String? = null
        private set

    val possibleToLogin: Boolean
        get() = tokens.state == TokensState.Valid && username != null

    init {
        client.setClientLoginListener(this)
        client.setClientSessionListener(this)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                return@addOnCompleteListener
            }
            firebaseToken = task.result
        }
    }

    fun login(username: String, password: String) {
        this.username = username
        this.password = password
        if (clientState == ClientState.DISCONNECTED) {
            try {
                client.connect()
            } catch (e: IllegalStateException) {
                Log.e(APP_TAG, "exception on connect: $e")
            }
        } else if (clientState == ClientState.CONNECTED) {
            client.login(username, password)
        }
    }

    fun loginWithToken() {
        when (clientState) {
            ClientState.LOGGED_IN -> listener?.onLoginSuccess(displayName.orEmpty())
            ClientState.DISCONNECTED ->
                try {
                    client.connect()
                } catch (e: IllegalStateException) {
                    Log.e(APP_TAG, "exception on connect: $e")
                }
            ClientState.CONNECTED -> {
                username = USERNAME.getStringFromPrefs(appContext).orEmpty()
                when (tokens.state) {
                    TokensState.Valid -> client.loginWithAccessToken(username, tokens.access)
                    TokensState.NeedsRefresh -> client.refreshToken(username, tokens.refresh)
                    TokensState.Expired -> listener?.onLoginFailed(AuthError.TokenExpired)
                }
            }
            else -> listener?.onLoginFailed(AuthError.InternalError)
        }
    }

    fun reconnectIfNeeded(completion: (AuthError?) -> Unit) {
        if (clientState == ClientState.DISCONNECTED && Shared.foregroundCheck.isInForeground) {
            if (possibleToLogin) {
                reconnectCompletion = completion
                loginWithToken()
            } else {
                completion(AuthError.TokenExpired)
            }
        } else {
            completion(null)
        }
    }

    private var reconnectCompletion: ((AuthError?) -> Unit)? = null

    fun logout() {
        if (clientState == ClientState.LOGGED_IN) {
            enablePushNotifications(false)
            displayName = null
            tokens.removeTokens()
            client.disconnect()
            MainActivity.OUTGOING_USERNAME.removeKeyFromPrefs(appContext)
        }
    }

    fun firebaseTokenRefreshed(token: String?) {
        firebaseToken = token
        enablePushNotifications(true)
    }

    fun pushNotificationReceived(message: Map<String?, String?>?) {
        client.handlePushNotification(message)
        loginWithToken()
    }

    private fun enablePushNotifications(enable: Boolean) {
        if (enable) {
            client.registerForPushNotifications(firebaseToken, null)
        } else {
            client.unregisterFromPushNotifications(firebaseToken, null)
        }
    }

    override fun onConnectionEstablished() {
        if (username != null && password != null) {
            client.login(username, password)
        } else {
            loginWithToken()
        }
    }

    @Synchronized
    override fun onConnectionFailed(error: String) {
        reconnectCompletion?.invoke(AuthError.NetworkIssues)
        reconnectCompletion = null
        listener?.onConnectionFailed(AuthError.NetworkIssues)
    }

    @Synchronized
    override fun onConnectionClosed() {
        listener?.onConnectionClosed()
    }

    @Synchronized
    override fun onLoginSuccessful(
        displayName: String?,
        authParams: AuthParams?
    ) {
        enablePushNotifications(true)
        this.displayName = displayName ?: this.username?.split('@')?.first()
        if (authParams != null)
            tokens.updateTokens(authParams)
        else Log.i(APP_TAG, "authParams not set")
        reconnectCompletion?.invoke(null)
        reconnectCompletion = null
        listener?.onLoginSuccess(this.displayName.orEmpty())
    }

    @Synchronized
    override fun onLoginFailed(reason: LoginError) {
        reconnectCompletion?.invoke(makeAuthError(reason))
        reconnectCompletion = null
        listener?.onLoginFailed(makeAuthError(reason))
    }

    override fun onOneTimeKeyGenerated(key: String) {}

    override fun onRefreshTokenFailed(reason: LoginError) {
        listener?.onLoginFailed(makeAuthError(reason))
    }

    override fun onRefreshTokenSuccess(authParams: AuthParams) {
        tokens.updateTokens(authParams)
        loginWithToken()
    }

    companion object {
        private const val USERNAME = "username"
    }
}
