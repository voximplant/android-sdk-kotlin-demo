/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.utils

import android.annotation.SuppressLint
import com.voximplant.demos.kotlin.services.*
import com.voximplant.sdk.hardware.ICameraManager
import org.webrtc.EglBase

object Shared {
    var appInForeground: Boolean = false
    lateinit var voximplantCallManager: VoximplantCallManager

    lateinit var fileLogger: FileLogger
    lateinit var authService: AuthService
    @SuppressLint("StaticFieldLeak")
    lateinit var notificationHelper: NotificationHelper
    lateinit var cameraManager: ICameraManager
    lateinit var shareHelper: ShareHelper
    @SuppressLint("StaticFieldLeak")
    lateinit var getResource: GetResource

    lateinit var eglBase: EglBase
}