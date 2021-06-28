package com.voximplant.demos.kotlin.videocall_deepar.utils

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
            InvalidPassword -> "The password is incorrect"
            InvalidUsername -> "The username is incorrect"
            AccountFrozen -> "Login failed due to account is frozen. Please contact your admin"
            TokenExpired -> "Login failed due to token being expired. Please use password"
            Timeout -> "Login failed due to timeout. Please try again"
            MauAssessDenied -> "Login failed due to MAU limit is reached"
            NetworkIssues -> "Login failed due to network issues. Please try again"
            else -> "Login failed due to internal error. Please try again"
        }
}

class CallManagerException(message: String): Exception(message)

val noActiveCallError: CallManagerException
    get() = CallManagerException("No active call found")
val alreadyManagingCallError: CallManagerException
    get() = CallManagerException("Already managing a call")
val callCreationError: CallManagerException
    get() = CallManagerException("There was an error creating call")
fun callManagerException(callException: CallException): CallManagerException =
    CallManagerException(callException.message ?: callException.localizedMessage)
