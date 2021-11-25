/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.telecom.CallAudioState
import android.telecom.Connection
import android.util.Log
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.Shared

class CallConnection : Connection() {

    override fun onShowIncomingCallUi() {
        Log.i(APP_TAG, "CallConnection::onShowIncomingCallUi")
        audioCallManager.showIncomingCallUI()
    }

    override fun onStateChanged(state: Int) {
        Log.i(APP_TAG, "CallConnection::onStateChanged ${stateToString(state)}")
        if (state == STATE_DISCONNECTED) {
            Log.i(APP_TAG, "CallConnection:: DisconnectCause: $disconnectCause")
        }
    }

    override fun onAnswer() {
        if (Shared.appInForeground) {
            Log.i(APP_TAG, "CallConnection::onAnswer from foreground")
            audioCallManager.answerIncomingCall()
        } else {
            Log.i(APP_TAG, "CallConnection::onAnswer from background")
            audioCallManager.showIncomingCallFragment(answer = true)
        }
    }

    override fun onAbort() {
        Log.i(APP_TAG, "CallConnection::onAbort")
        audioCallManager.hangupOngoingCall()
    }

    override fun onReject() {
        Log.i(APP_TAG, "CallConnection::onReject")
        audioCallManager.declineIncomingCall()
    }

    override fun onReject(rejectReason: Int) {
        Log.i(APP_TAG, "CallConnection::onReject with reason: $rejectReason")
        audioCallManager.declineIncomingCall()
    }

    override fun onReject(replyMessage: String?) {
        Log.i(APP_TAG, "CallConnection::onReject with reply message: $replyMessage")
        audioCallManager.declineIncomingCall()
    }

    override fun onDisconnect() {
        Log.i(APP_TAG, "CallConnection::onDisconnect")
        audioCallManager.hangupOngoingCall()
    }

    override fun onHold() {
        Log.i(APP_TAG, "CallConnection::onHold")
        audioCallManager.holdOngoingCall(true)
    }

    override fun onUnhold() {
        Log.i(APP_TAG, "CallConnection::onUnhold")
        audioCallManager.holdOngoingCall(false)
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        Log.i(APP_TAG, "CallConnection::onCallAudioStateChanged $state")
    }

    init {
        setInitializing()
        connectionProperties = PROPERTY_SELF_MANAGED
        audioModeIsVoip = true
        connectionCapabilities += CAPABILITY_HOLD
        connectionCapabilities += CAPABILITY_SUPPORT_HOLD
        connectionCapabilities += CAPABILITY_MUTE
    }
}