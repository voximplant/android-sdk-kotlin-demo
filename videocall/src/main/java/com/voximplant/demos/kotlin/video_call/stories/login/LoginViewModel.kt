/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.login

import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.services.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.client.Node

private val String.appendingVoxDomain get() = "$this.voximplant.com"

class LoginViewModel : BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService
    val didLogin = MutableLiveData<Unit>()
    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val node = MutableLiveData<Node?>()
    val usernameFieldError = MutableLiveData<Int?>()
    val passwordFieldError = MutableLiveData<Int?>()
    val nodeFieldError = MutableLiveData<Int?>()

    init {
        authService.listener = this
    }

    fun login(username: String?, password: String?) {
        val node = this.node.value

        when {
            username.isNullOrEmpty() -> usernameFieldError.postValue(R.string.required_field)
            password.isNullOrEmpty() -> passwordFieldError.postValue(R.string.required_field)
            node == null -> nodeFieldError.postValue(R.string.required_field)
            else -> authService.login(username.appendingVoxDomain, password, node)
        }
    }

    fun changeNode(node: Node) {
        this.node.value = node
        nodeFieldError.postValue(null)
    }

    override fun onCreate() {
        super.onCreate()
        if (authService.possibleToLogin) {
            showProgress.postValue(R.string.logging_in)
            authService.loginWithToken()
        }
    }

    override fun onResume() {
        super.onResume()
        username.postValue(Shared.authService.username?.substringBefore(".voximplant.com"))
        password.postValue("")
    }

    override fun onLoginSuccess(displayName: String) {
        super.onLoginSuccess(displayName)
        hideProgress.postValue(Unit)
        didLogin.postValue(Unit)
    }

    override fun onLoginFailed(error: AuthError) {
        super.onLoginFailed(error)
        hideProgress.postValue(Unit)
        when (error) {
            AuthError.InvalidUsername -> usernameFieldError.postValue(R.string.invalid_username_warning)
            AuthError.InvalidPassword -> passwordFieldError.postValue(R.string.invalid_password_warning)
            else -> postError(error)
        }
    }

    override fun onConnectionFailed(error: AuthError) {
        super.onConnectionFailed(error)
        hideProgress.postValue(Unit)
        postError(error)
    }

    override fun onConnectionClosed() {
        super.onConnectionClosed()
        hideProgress.postValue(Unit)
    }

    override fun onCleared() {
        super.onCleared()
        authService.listener = null
    }
}
