package com.voximplant.demos.kotlin.video_call.stories.login

import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.services.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.utils.*

private val String.appendingVoxDomain get() = "$this.voximplant.com"

class LoginViewModel : BaseViewModel(), AuthServiceListener {
    private val authService: AuthService = Shared.authService
    val didLogin = MutableLiveData<Unit>()
    val invalidInputError = MutableLiveData<Pair<Boolean, Int>>()
    val usernameFieldText = MutableLiveData<String>()
    val passwordFieldText = MutableLiveData<String>()

    init {
        authService.listener = this
    }

    fun login(username: String?, password: String?) {
        when {
            username.isNullOrEmpty() -> invalidInputError.postValue(
                Pair(
                    true,
                    R.string.empty_field_warning
                )
            )
            password.isNullOrEmpty() -> invalidInputError.postValue(
                Pair(
                    false,
                    R.string.empty_field_warning
                )
            )
            else -> authService.login(username.appendingVoxDomain, password)
        }
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
        usernameFieldText.postValue(Shared.authService.username?.substringBefore(".voximplant.com"))
        passwordFieldText.postValue("")
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
            AuthError.InvalidUsername -> invalidInputError.postValue(
                Pair(
                    true,
                    R.string.invalid_username_warning
                )
            )
            AuthError.InvalidPassword -> invalidInputError.postValue(
                Pair(
                    false,
                    R.string.invalid_password_warning
                )
            )
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
