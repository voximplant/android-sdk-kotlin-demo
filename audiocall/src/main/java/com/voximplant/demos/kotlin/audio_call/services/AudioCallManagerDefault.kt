/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.services

import android.content.Context
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.call.*
import com.voximplant.sdk.client.IClient
import java.util.*

class AudioCallManagerDefault(
    context: Context,
    client: IClient,
) : AudioCallManager(context, client) {

    override fun onIncomingCall(call: ICall, video: Boolean, headers: Map<String?, String?>?) {
        super.onIncomingCall(call, video, headers)
        showIncomingCallUI()
    }

    @Throws(CallManagerException::class)
    override fun startOutgoingCall() = startOutgoingCallInternal()

    override fun showIncomingCallUI() {
        if (callExists) {
            showIncomingCallNotification()
            if (Shared.appInForeground) {
                showIncomingCallFragment()
            }
        }
    }

}
