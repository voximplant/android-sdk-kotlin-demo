/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG

class CallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?
    ): Connection? {
        Log.i(APP_TAG, "CallConnectionService::onCreateIncomingConnection $request")
        return if (audioCallManager is AudioCallManagerWithTelecom) {
            (audioCallManager as AudioCallManagerWithTelecom).createIncomingConnection()?.apply {
                setRinging()
            }
        } else {
            null
        }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ) {
        Log.e(APP_TAG, "CallConnectionService::onCreateIncomingConnectionFailed $request")
        audioCallManager.showIncomingCallUI()
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?,
    ): Connection? {
        Log.i(APP_TAG, "CallConnectionService::onCreateOutgoingConnection $request")
        return if (audioCallManager is AudioCallManagerWithTelecom) {
            (audioCallManager as AudioCallManagerWithTelecom).createOutgoingConnection()
        } else {
            return null
        }
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?
    ) {
        Log.e(APP_TAG, "CallConnectionService::onCreateOutgoingConnectionFailed $request")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(APP_TAG, "CallConnectionService::onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(APP_TAG, "CallConnectionService::onDestroy")
    }
}
