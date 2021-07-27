package com.voximplant.demos.kotlin.utils

import com.voximplant.demos.kotlin.utils.Shared.getResource
import com.voximplant.sdk.call.CallException
import com.voximplant.sdk.client.LoginError

fun makeAuthError(loginError: LoginError): AuthError =
    when (loginError) {
        LoginError.INVALID_PASSWORD -> AuthError.InvalidPassword
        LoginError.INVALID_USERNAME -> AuthError.InvalidUsername
        LoginError.ACCOUNT_FROZEN -> AuthError.AccountFrozen
        LoginError.INTERNAL_ERROR -> AuthError.InternalError
        LoginError.INVALID_STATE -> AuthError.InvalidState
        LoginError.NETWORK_ISSUES -> AuthError.NetworkIssues
        LoginError.TOKEN_EXPIRED -> AuthError.TokenExpired
        LoginError.TIMEOUT -> AuthError.Timeout
        LoginError.MAU_ACCESS_DENIED -> AuthError.MauAssessDenied
    }

enum class AuthError {
    InvalidPassword,
    InvalidUsername,
    AccountFrozen,
    InternalError,
    TokenExpired,
    Timeout,
    NetworkIssues,
    MauAssessDenied,
    InvalidState;

    val description: String
        get() = when (this) {
            InvalidPassword -> getResource.getString(R.string.error_incorrect_password)
            InvalidUsername -> getResource.getString(R.string.error_incorrect_username)
            AccountFrozen -> getResource.getString(R.string.error_login_failed_account_frozen)
            TokenExpired -> getResource.getString(R.string.error_login_failed_token_expired)
            Timeout -> getResource.getString(R.string.error_login_failed_timeout)
            MauAssessDenied -> getResource.getString(R.string.error_login_failed_mau_limit)
            NetworkIssues -> getResource.getString(R.string.error_login_failed_network_issues)
            else -> getResource.getString(R.string.error_login_failed_internal_error)
        }
}

class CallManagerException(message: String) : Exception(message)

val noActiveCallError: CallManagerException
    get() = CallManagerException(getResource.getString(R.string.error_no_active_call))
val alreadyManagingCallError: CallManagerException
    get() = CallManagerException(getResource.getString(R.string.error_already_managing_call))
val callCreationError: CallManagerException
    get() = CallManagerException(getResource.getString(R.string.error_creating_call))

fun callManagerException(callException: CallException): CallManagerException =
    CallManagerException(callException.message ?: callException.localizedMessage)
