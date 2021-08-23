/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.audio_call.telecomManager
import com.voximplant.demos.kotlin.audio_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.audio_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.audio_call.utils.CallState
import com.voximplant.demos.kotlin.services.CallService
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import com.voximplant.sdk.client.IClientIncomingCallListener
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class AudioCallManager(
    private val appContext: Context,
    private val client: IClient,
) : IClientIncomingCallListener, ICallListener, IEndpointListener {

    // Call management
    private var managedCall: ICall? = null
    val callExists: Boolean
        get() = managedCall != null
    val callerDisplayName
        get() = managedCall?.endpoints?.get(0)?.userDisplayName
    var managedCallConnection: CallConnection? = null
    private var callTimer: Timer = Timer("callTimer")
    val callDuration = MutableLiveData(0L)
    val callBroadcastReceiver: BroadcastReceiver = AudioCallBroadcastReceiver()
    private val callSettings: CallSettings
        get() = CallSettings().apply { videoFlags = VideoFlags(false, false) }

    // Call properties
    var latestCallerDisplayName: String? = null
        private set
    var latestCallerUsername: String? = null
        private set
    var muted: Boolean = false
        private set
    private val _onHold = MutableLiveData(false)
    val onHold: LiveData<Boolean> = _onHold

    // Call events
    var onCallDisconnect: ((failed: Boolean, reason: String) -> Unit)? = null
    var onCallConnect: (() -> Unit)? = null
    private var callState: CallState? = null

    init {
        client.setClientIncomingCallListener(this)
    }

    fun createConnection(): CallConnection? {
        managedCallConnection = CallConnection()
        managedCallConnection?.setInitialized()
        Log.i(APP_TAG, "AudioCallManager::createConnection: Connection created & initialized")
        return managedCallConnection
    }

    override fun onIncomingCall(call: ICall, video: Boolean, headers: Map<String?, String?>?) {
        if (callExists) {
            // App will reject incoming calls if already have one, because it supports only single managed call at a time.
            try {
                call.reject(RejectMode.DECLINE, null)
            } catch (e: CallException) {
                Log.e(APP_TAG, "AudioCallManager::onIncomingCall ${e.message}")
            }
            return
        }
        callState = CallState.NEW
        call.also {
            it.addCallListener(this)
            managedCall = it
            latestCallerUsername = it.endpoints.firstOrNull()?.userName
            latestCallerDisplayName = it.endpoints.firstOrNull()?.userDisplayName
        }
        telecomManager.addIncomingCall()
    }

    override fun onCallConnected(call: ICall?, headers: Map<String?, String?>?) {
        managedCallConnection?.setActive()
        callState = CallState.CONNECTED
        onCallConnect?.invoke()
        Shared.notificationHelper.cancelIncomingCallNotification()
        callTimer = Timer("callTimer").apply {
            scheduleAtFixedRate(delay = TIMER_DELAY_MS, TIMER_DELAY_MS) {
                callDuration.postValue(call?.callDuration)
            }
        }
        latestCallerDisplayName = call?.endpoints?.firstOrNull()?.userDisplayName
        startForegroundCallService()
    }

    override fun onCallDisconnected(
        call: ICall,
        headers: Map<String?, String?>?,
        answeredElsewhere: Boolean,
    ) {
        Log.i(APP_TAG, "AudioCallManager::onCallDisconnected answeredElsewhere: $answeredElsewhere")
        removeCall()
        when {
            answeredElsewhere -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE))
            }
            callState == CallState.DISCONNECTING -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            }
            callState == CallState.CONNECTED -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            }
            callState == CallState.NEW -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.MISSED))
            }
            else -> {
                managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.OTHER))
            }
        }
        callState = CallState.DISCONNECTED
        managedCallConnection?.destroy()
        onCallDisconnect?.invoke(false, appContext.getString(R.string.disconnected))
    }

    override fun onCallFailed(
        call: ICall,
        code: Int,
        description: String,
        headers: Map<String?, String?>?,
    ) {
        Log.i(APP_TAG, "AudioCallManager::onCallFailed code: $code, description: $description")
        removeCall()
        when (code) {
            486 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.BUSY))
            603 -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            else -> managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.ERROR))
        }
        callState = CallState.DISCONNECTED
        managedCallConnection?.destroy()
        onCallDisconnect?.invoke(true, description)
    }

    override fun onCallRinging(call: ICall?, headers: Map<String, String>?) {
        managedCallConnection?.setDialing()
    }

    override fun onEndpointAdded(call: ICall, endpoint: IEndpoint) =
        endpoint.setEndpointListener(this)

    @Throws(CallManagerException::class)
    fun createOutgoingCall(user: String) =
        executeOrThrow {
            // App won't start new call if already have one, because it supports only single managed call at a time.
            if (callExists) {
                throw alreadyManagingCallError
            }
            callState = CallState.NEW
            managedCall = client.call(user, callSettings)?.also {
                latestCallerUsername = user
                telecomManager.addOutgoingCall(user)
                it.addCallListener(this)
            }
                ?: throw callCreationError
        }

    @Throws(CallManagerException::class)
    fun startOutgoingCall() =
        executeOrThrow {
            callDuration.postValue(0)
            callState = CallState.CONNECTING
            managedCall?.start() ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun answerIncomingCall() =
        executeOrThrow {
            callDuration.postValue(0)
            callState = CallState.CONNECTING
            managedCall?.answer(callSettings)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun declineIncomingCall() =
        executeOrThrow {
            Shared.notificationHelper.cancelIncomingCallNotification()
            callState = CallState.DISCONNECTING
            managedCall?.reject(RejectMode.DECLINE, null)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun muteOngoingCall(mute: Boolean) =
        executeOrThrow {
            managedCall?.sendAudio(!mute).also { muted = mute }
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun holdOngoingCall(hold: Boolean) {
        executeOrThrow {
            managedCall?.hold(hold, object : ICallCompletionHandler {
                override fun onComplete() {
                    _onHold.postValue(hold)
                    if (hold) {
                        managedCallConnection?.setOnHold()
                    } else {
                        managedCallConnection?.setActive()
                    }
                    Shared.notificationHelper.updateOngoingNotification(
                        hold,
                        latestCallerUsername,
                    )
                }

                override fun onFailure(e: CallException) {}
            }) ?: throw noActiveCallError
        }
    }

    @Throws(CallManagerException::class)
    fun hangupOngoingCall() =
        executeOrThrow {
            managedCallConnection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            callState = CallState.DISCONNECTING
            managedCall?.hangup(null)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    fun sendDTMF(DTMF: String) =
        executeOrThrow {
            managedCall?.sendDTMF(DTMF)
                ?: throw noActiveCallError
        }

    @Throws(CallManagerException::class)
    private fun executeOrThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: CallException) {
            throw callManagerException(e)
        } catch (e: CallManagerException) {
            throw e
        } catch (e: Exception) {
            throw CallManagerException(e.message ?: "An internal error")
        }
    }

    private fun removeCall() {
        stopForegroundService()
        Shared.notificationHelper.cancelIncomingCallNotification()
        Shared.notificationHelper.cancelOngoingCallNotification()
        managedCall?.removeCallListener(this)
        managedCall?.endpoints?.firstOrNull()?.setEndpointListener(null)
        managedCall = null
        callTimer.purge()
        callTimer.cancel()
        _onHold.postValue(false)
    }

    /// Service

    private fun startForegroundCallService() {
        Handler(Looper.getMainLooper()).post {
            val filter = IntentFilter().apply {
                addAction(ACTION_HANGUP_ONGOING_CALL)
            }
            appContext.registerReceiver(callBroadcastReceiver, filter)
            Shared.notificationHelper.createOngoingCallNotification(
                appContext,
                latestCallerDisplayName ?: latestCallerUsername,
                appContext.getString(R.string.call_in_progress),
                MainActivity::class.java,
            )

            Intent(appContext, CallService::class.java).let {
                it.action = ACTION_FOREGROUND_SERVICE_START
                appContext.startService(it)
            }
        }
    }

    private fun stopForegroundService() {
        Intent(appContext, CallService::class.java).let {
            it.action = ACTION_FOREGROUND_SERVICE_STOP
            appContext.stopService(it)
        }
    }

    /// Incoming call visuals

    fun showIncomingCallUI() {
        if (callExists) {
            showIncomingCallNotification()
            if (Shared.appInForeground) {
                showIncomingCallFragment()
            }
        } else {
            managedCallConnection?.destroy()
        }
    }

    private fun showIncomingCallNotification() {
        Intent(appContext, CallActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            val filter = IntentFilter().apply {
                addAction(ACTION_ANSWER_INCOMING_CALL)
                addAction(ACTION_DECLINE_INCOMING_CALL)
            }
            appContext.registerReceiver(callBroadcastReceiver, filter)
            Shared.notificationHelper.showIncomingCallNotification(
                appContext,
                intent,
                latestCallerDisplayName ?: appContext.getString(R.string.unknown_user),
            )
        }
    }

    fun showIncomingCallFragment(answer: Boolean = false) {
        Intent(appContext, CallActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(IS_INCOMING_CALL, true)
            it.putExtra(ACTION_ANSWER_INCOMING_CALL, answer)
            startActivity(appContext, it, null)
        }
    }
}

private class AudioCallActionReceiver(private val onReceive: (AudioCallActionReceiver) -> Unit) :
    BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) = onReceive(this)
}

class AudioCallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_INCOMING_CALL -> audioCallManager.showIncomingCallFragment(answer = true)
            ACTION_DECLINE_INCOMING_CALL -> audioCallManager.declineIncomingCall()
            ACTION_HANGUP_ONGOING_CALL -> audioCallManager.hangupOngoingCall()
        }
    }
}