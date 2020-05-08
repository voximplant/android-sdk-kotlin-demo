package com.voximplant.demos.kotlin.video_call.services

import android.content.Context
import android.util.Log
import com.voximplant.demos.kotlin.video_call.utils.*
import com.voximplant.sdk.client.AuthParams

private const val REFRESH_TIME = "refreshTime"
private const val ACCESS_TOKEN = "accessToken"
private const val ACCESS_EXPIRE = "accessExpire"
private const val REFRESH_TOKEN = "refreshToken"
private const val REFRESH_EXPIRE = "refreshExpire"

private const val MILLISECONDS_IN_SECOND = 1000

enum class TokensState {
    Valid,
    NeedsRefresh,
    Expired
}

class Tokens(private val context: Context) {
    val access: String get() = ACCESS_TOKEN.getStringFromPrefs(context).orEmpty()
    val refresh: String get() = REFRESH_TOKEN.getStringFromPrefs(context).orEmpty()

    // must be checked before access to access/refresh fields
    val state: TokensState get() {
        if (!tokensValid) { return TokensState.Expired }
        if (accessExpired) { return TokensState.NeedsRefresh }
        return TokensState.Valid
    }

    private val tokensExist: Boolean get() =
        ACCESS_TOKEN.getStringFromPrefs(context) != null && REFRESH_TOKEN.getStringFromPrefs(context) != null
    private val accessExpired: Boolean get() = tokenExpired(ACCESS_EXPIRE.getLongFromPrefs(context))
    private val refreshExpired: Boolean get() = tokenExpired(REFRESH_EXPIRE.getLongFromPrefs(context))
    private fun tokenExpired(expireDate: Long): Boolean =
        System.currentTimeMillis() - REFRESH_TIME.getLongFromPrefs(context) >= expireDate * MILLISECONDS_IN_SECOND
    private val tokensValid: Boolean get() = if (tokensExist) { !refreshExpired } else { false }

    fun updateTokens(authParams: AuthParams) {
        authParams.accessToken.saveToPrefs(context, ACCESS_TOKEN)
        authParams.accessTokenTimeExpired.toLong().saveToPrefs(context, ACCESS_EXPIRE)
        authParams.refreshToken.saveToPrefs(context, REFRESH_TOKEN)
        authParams.refreshTokenTimeExpired.toLong().saveToPrefs(context, REFRESH_EXPIRE)
        System.currentTimeMillis().saveToPrefs(context, REFRESH_TIME)
    }

    fun removeTokens() {
        ACCESS_TOKEN.removeKeyFromPrefs(context)
        ACCESS_EXPIRE.removeKeyFromPrefs(context)
        REFRESH_TOKEN.removeKeyFromPrefs(context)
        REFRESH_EXPIRE.removeKeyFromPrefs(context)
        REFRESH_TIME.removeKeyFromPrefs(context)
    }
}