/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.Manifest
import android.content.Context
import android.telecom.DisconnectCause
import android.util.Log
import androidx.annotation.RequiresPermission
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import java.util.*

class AudioCallManagerWithTelecom(
    context: Context,
    client: IClient,
) : AudioCallManager(context, client) {
    private val telecomManager = TelecomManager(context).apply { registerAccount() }

    private var managedCallConnection: CallConnection? = null

    fun createIncomingConnection(): CallConnection? {
        Log.i(APP_TAG, "AudioCallManagerWithTelecom::createIncomingConnection")
        managedCallConnection = CallConnection()
        managedCallConnection?.setInitialized()
        audioDeviceManager.setTelecomConnection(managedCallConnection)
        return managedCallConnection
    }

    fun createOutgoingConnection(): CallConnection? {
        Log.i(APP_TAG, "AudioCallManagerWithTelecom::createOutgoingConnection")
        managedCallConnection = CallConnection()
        managedCallConnection?.setInitialized()
        audioDeviceManager.setTelecomConnection(managedCallConnection)
        managedCall?.start() ?: throw noActiveCallError
        return managedCallConnection
    }

    override fun onIncomingCall(call: ICall, video: Boolean, headers: Map<String?, String?>?) {
        super.onIncomingCall(call, video, headers)
        telecomManager.addIncomingCall()
    }

    override fun onCallConnected(call: ICall?, headers: Map<String?, String?>?) {
        managedCallConnection?.setActive()
        super.onCallConnected(call, headers)
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean,
    ) {
        super.onCallDisconnected(call, headers, answeredElsewhere)
        when {
            answeredElsewhere -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE))
            }

            callState.value == CallState.DISCONNECTING -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            }

            callState.value == CallState.CONNECTED -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            }

            callState.value == CallState.INCOMING -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
            }

            else -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.OTHER))
            }
        }
        managedCallConnection?.destroy()
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?,
    ) {
        super.onCallFailed(call, code, description, headers)
        when (code) {
            486 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.BUSY))
            603 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            else -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
        }
        managedCallConnection?.destroy()
    }

    override fun onCallRinging(call: ICall?, headers: Map<String, String>?) {
        super.onCallRinging(call, headers)
        managedCallConnection?.setDialing()
    }

    @RequiresPermission(anyOf = [Manifest.permission.CALL_PHONE, Manifest.permission.MANAGE_OWN_CALLS])
    override fun startOutgoingCall() {
        super.startOutgoingCall()
        endpointUsername?.let { username -> telecomManager.addOutgoingCall(username) }
    }

    override fun holdOngoingCall(hold: Boolean, onCompletion: () -> Unit) {
        super.holdOngoingCall(
            hold,
            onCompletion = {
                if (hold) {
                    managedCallConnection?.setOnHold()
                } else {
                    managedCallConnection?.setActive()
                }
            },
        )
    }

    override fun answerIncomingCall() {
        if (callState.value == CallState.RECONNECTING) {
            //  setActive() is required here to notify wearable devices that the incoming call has been answered.
            managedCallConnection?.setActive()
        }
        super.answerIncomingCall()
    }

    @Throws(CallManagerException::class)
    override fun hangupOngoingCall() {
        managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        super.hangupOngoingCall()
    }

    override fun showIncomingCallUI() {
        if (callExists) {
            showIncomingCallNotification()
            if (Shared.appInForeground) {
                showIncomingCallFragment()
            }
        } else {
            managedCallConnection?.destroy()
        }
    }
}
