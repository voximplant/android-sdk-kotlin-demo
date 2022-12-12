/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.Shared.phoneAccount

class TelecomManager(private val context: Context) {
    private val telecomManager: TelecomManager =
        context.getSystemService(AppCompatActivity.TELECOM_SERVICE) as TelecomManager

    private fun getAccountHandle(): PhoneAccountHandle {
        val componentName = ComponentName(context, CallConnectionService::class.java)
        return PhoneAccountHandle(componentName, APP_TAG)
    }

    fun registerAccount() {
        val accountHandle = getAccountHandle()
        val builder = PhoneAccount.builder(accountHandle, APP_TAG)
        builder.setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
        builder.addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
        builder.setIcon(Icon.createWithResource(context, R.drawable.ic_vox_notification))
        phoneAccount = builder.build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    fun addIncomingCall() {
        if (phoneAccount != null) {
            if (telecomManager.isIncomingCallPermitted(phoneAccount?.accountHandle)) {
                Log.i(APP_TAG, "TelecomManager::addIncomingCall")
                val extras = Bundle()
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount?.accountHandle)
                telecomManager.addNewIncomingCall(phoneAccount?.accountHandle, extras)
            } else {
                Log.e(APP_TAG, "TelecomManager::addIncomingCall: Incoming call not permitted")
            }
        } else {
            Log.w(APP_TAG, "TelecomManager::addIncomingCall: Couldn't add incoming call. Account not registered")
            audioCallManager.showIncomingCallUI()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.CALL_PHONE, Manifest.permission.MANAGE_OWN_CALLS])
    fun addOutgoingCall(userName: String) {
        if (phoneAccount != null) {
            if (telecomManager.isOutgoingCallPermitted(phoneAccount?.accountHandle)) {
                Log.i(APP_TAG, "TelecomManager::addOutgoingCall")
                val uri: Uri = Uri.fromParts(PhoneAccount.SCHEME_SIP, userName, null)
                val extras = Bundle()
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccount?.accountHandle)
                telecomManager.placeCall(uri, extras)
            } else {
                Log.e(APP_TAG, "TelecomManager::addOutgoingCall: Outgoing call not permitted")
            }
        } else {
            Log.w(APP_TAG, "TelecomManager::addOutgoingCall: Couldn't add outgoing call. Account not registered")
        }
    }
}