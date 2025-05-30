/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.services

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.AuthError
import com.voximplant.demos.kotlin.utils.LAST_OUTGOING_CALL_USERNAME
import com.voximplant.demos.kotlin.utils.getLongFromPrefs
import com.voximplant.demos.kotlin.utils.getStringFromPrefs
import com.voximplant.demos.kotlin.utils.makeAuthError
import com.voximplant.demos.kotlin.utils.removeKeyFromPrefs
import com.voximplant.demos.kotlin.utils.saveToPrefs
import com.voximplant.sdk.client.AuthParams
import com.voximplant.sdk.client.ClientState
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientLoginListener
import com.voximplant.sdk.client.IClientSessionListener
import com.voximplant.sdk.client.IPushTokenCompletionHandler
import com.voximplant.sdk.client.LoginError
import com.voximplant.sdk.client.Node
import com.voximplant.sdk.client.PushTokenError

class AuthService(
    private val client: IClient,
    private val appContext: Context
) : IClientSessionListener, IClientLoginListener {
    private val tokens = Tokens(appContext)
    private val clientState: ClientState
        get() = client.clientState
    var listener: AuthServiceListener? = null
    var username: String?
        get() = USERNAME.getStringFromPrefs(appContext)
        private set(newValue) =
            if (newValue == null) {
                USERNAME.removeKeyFromPrefs(appContext)
            } else {
                newValue.saveToPrefs(appContext, USERNAME)
            }
    var node: Node?
        get() = when (NODE.getLongFromPrefs(appContext)) {
            0L -> Node.NODE_1
            1L -> Node.NODE_2
            2L -> Node.NODE_3
            3L -> Node.NODE_4
            4L -> Node.NODE_5
            5L -> Node.NODE_6
            6L -> Node.NODE_7
            7L -> Node.NODE_8
            8L -> Node.NODE_9
            9L -> Node.NODE_10
            10L -> Node.NODE_11
            else -> null
        }
        private set(value) {
            when (value) {
                Node.NODE_1 -> 0L
                Node.NODE_2 -> 1L
                Node.NODE_3 -> 2L
                Node.NODE_4 -> 3L
                Node.NODE_5 -> 4L
                Node.NODE_6 -> 5L
                Node.NODE_7 -> 6L
                Node.NODE_8 -> 7L
                Node.NODE_9 -> 8L
                Node.NODE_10 -> 9L
                Node.NODE_11 -> 10L
                null -> null
            }.let { node ->
                if (node != null) {
                    node.saveToPrefs(appContext, NODE)
                } else {
                    NODE.removeKeyFromPrefs(appContext)
                }
            }
        }
    private var password: String? = null
    private var firebaseToken: String? = null
    var displayName: String?
        get() = DISPLAYNAME.getStringFromPrefs(appContext)
        private set(newValue) =
            if (newValue == null) {
                DISPLAYNAME.removeKeyFromPrefs(appContext)
            } else {
                newValue.saveToPrefs(appContext, DISPLAYNAME)
            }

    val possibleToLogin: Boolean
        get() = tokens.state == TokensState.Valid && username != null
    private var needToLogout = false

    init {
        client.setClientLoginListener(this)
        client.setClientSessionListener(this)
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful || task.result == null) {
                    return@addOnCompleteListener
                }
                firebaseToken = task.result
            }
        } catch (exception: java.lang.IllegalStateException) {
            Log.e(
                APP_TAG,
                "AuthService::init: FCM token is not received. Push notifications are disabled.\n" +
                        "Uncomment `plugin com.google.gms.google-services` line in app-level `build.gradle` " +
                        "and add `google-services.json` file into the app-level directory to enable it.",
                exception
            )
        }
    }

    fun login(username: String, password: String, node: Node) {
        this.username = username
        this.password = password
        this.node = node
        if (clientState == ClientState.DISCONNECTED) {
            try {
                client.connect(node)
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
                    node.let { node ->
                        if (node != null) {
                            client.connect(node)
                        } else {
                            Log.e(APP_TAG, "A node is required to connect")
                        }
                    }
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
        if (clientState == ClientState.DISCONNECTED) {
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
        needToLogout = true
        if (clientState == ClientState.LOGGED_IN) {
            enablePushNotifications(
                false,
                completion = {
                    displayName = null
                    tokens.removeTokens()
                    client.disconnect()
                    LAST_OUTGOING_CALL_USERNAME.removeKeyFromPrefs(appContext)
                },
            )
        } else {
            loginWithToken()
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

    private fun enablePushNotifications(enable: Boolean, completion: ((PushTokenError?) -> Unit)? = null) {
        val handler = object : IPushTokenCompletionHandler {
            override fun onSuccess() {
                completion?.invoke(null)
            }

            override fun onFailure(error: PushTokenError?) {
                completion?.invoke(error)
            }
        }

        if (enable) {
            client.registerForPushNotifications(firebaseToken, handler)
        } else {
            client.unregisterFromPushNotifications(firebaseToken, handler)
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
        if (needToLogout) {
            listener?.onLogout()
            needToLogout = false
        } else {
            listener?.onConnectionClosed()
        }
    }

    @Synchronized
    override fun onLoginSuccessful(
        displayName: String?,
        authParams: AuthParams?
    ) {
        if (needToLogout) {
            logout()
            return
        }
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
        private const val DISPLAYNAME = "displayname"
        private const val NODE = "node"
    }
}
