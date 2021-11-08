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
* Disconnecting -> ICall.hangup()
* Decline -> ICall.reject()
* Hang up -> ICall.hangup()
* Failed -> onCallFailed()
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
    DECLINE(R.string.call_state_decline),
    HANG_UP(R.string.call_state_hang_up),
    FAILED(R.string.call_state_failed),
    DISCONNECTED(R.string.call_state_disconnected),
    RECONNECTING(R.string.call_state_reconnecting),
    ON_HOLD(R.string.call_state_on_hold);

    override fun toString(): String {
        return getResource.getString(resourceId)
    }

}
