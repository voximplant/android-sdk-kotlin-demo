/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.utils
/*
*
* Call state usage
* NEW -> onIncomingCall(), IClient.call()
* Connecting -> ICall.answer(), ICall.start()
* Connected -> onCallConnected()
* Disconnecting -> ICall.hangup()
* Disconnected -> onCallDisconnected(), onCallFailed()
*
* */
enum class CallState { NEW, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED }