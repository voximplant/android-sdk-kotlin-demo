/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.telecom.*
import android.util.Log
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG

class CallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.i(APP_TAG, "CallConnectionService::onCreateIncomingConnection $request")
        return audioCallManager.createConnection().also { it?.setRinging() }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(APP_TAG, "CallConnectionService::onCreateIncomingConnectionFailed $request")
        audioCallManager.showIncomingCallUI()
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection? {
        Log.i(APP_TAG, "CallConnectionService::onCreateOutgoingConnection $request")
        return audioCallManager.createConnection()
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.e(APP_TAG, "CallConnectionService::onCreateOutgoingConnectionFailed $request")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(APP_TAG, "CallConnectionService:: created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(APP_TAG, "CallConnectionService:: destroyed")
    }
}
