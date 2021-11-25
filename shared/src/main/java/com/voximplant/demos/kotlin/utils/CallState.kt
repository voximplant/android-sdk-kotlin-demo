/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.utils

import com.voximplant.demos.kotlin.utils.Shared.getResource

/*
*
* Call state usage
* None -> On app launch
* Incoming -> onIncomingCall()
* Outgoing -> IClient.call()
* Connecting -> ICall.answer(), ICall.start(), onCallReconnected()
* Ringing -> onCallRinging()
* Connected -> onCallConnected()
* Failed -> onCallFailed()
* Disconnecting -> ICall.hangup(), ICall.reject()
* Disconnected -> onCallDisconnected(), onCallFailed()
* Reconnecting -> onCallReconnecting()
* On hold -> ICall.hold()
*
* */
enum class CallState(private val resourceId: Int) {
    NONE(R.string.call_state_none),
    INCOMING(R.string.call_state_incoming),
    OUTGOING(R.string.call_state_outgoing),
    CONNECTING(R.string.call_state_connecting),
    RINGING(R.string.call_state_ringing),
    CONNECTED(R.string.call_state_connected),
    FAILED(R.string.call_state_failed),
    DISCONNECTING(R.string.call_state_disconnecting),
    DISCONNECTED(R.string.call_state_disconnected),
    RECONNECTING(R.string.call_state_reconnecting);

    override fun toString(): String {
        return getResource.getString(resourceId)
    }

}
