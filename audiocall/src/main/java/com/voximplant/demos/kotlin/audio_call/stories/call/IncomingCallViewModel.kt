/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.CallManagerException
import com.voximplant.demos.kotlin.utils.Shared

class IncomingCallViewModel : ViewModel() {
    val displayName = MutableLiveData<String>()
    val finishActivity = MutableLiveData<Unit>()
    val moveToCall = MutableLiveData<Unit>()

    init {
        audioCallManager.onCallAnswer = {
            moveToCall.postValue(Unit)
        }

        audioCallManager.onCallDisconnect = { _, _ ->
            finishActivity.postValue(Unit)
        }

        displayName.postValue(
            audioCallManager.latestCallerDisplayName
                ?: audioCallManager.latestCallerUsername.orEmpty()
        )
    }

    fun viewCreated() {
        // The call can be canceled before the fragment is created
        if (!audioCallManager.callExists) {
            Log.w(APP_TAG, "IncomingCallViewModel::checkCallExistence The call no longer exists")
            Shared.notificationHelper.cancelIncomingCallNotification()
            finishActivity.postValue(Unit)
        }
    }

    fun answer() = audioCallManager.answerIncomingCall()

    fun decline() {
        try {
            audioCallManager.declineIncomingCall()
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
        }
        finishActivity.postValue(Unit)
    }
}