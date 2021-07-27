/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voximplant.demos.kotlin.audio_call.audioCallManager
import com.voximplant.demos.kotlin.utils.*

class CallFailedViewModel : ViewModel() {
    val displayName = MutableLiveData<String>()
    val moveToCall = MutableLiveData<Unit>()
    val finishActivity = MutableLiveData<Unit>()

    init {
        displayName.postValue(
            audioCallManager.latestCallerDisplayName ?: audioCallManager.latestCallerUsername.orEmpty()
        )
    }

    fun cancel() {
        finishActivity.postValue(Unit)
    }

    fun callBack() {
        try {
            audioCallManager.createOutgoingCall(audioCallManager.latestCallerUsername.orEmpty())
            moveToCall.postValue(Unit)
        } catch (e: CallManagerException) {
            Log.e(APP_TAG, e.message.toString())
            finishActivity.postValue(Unit)
        }
    }
}