package com.voximplant.demos.kotlin.video_call.utils

import com.voximplant.demos.kotlin.video_call.services.AuthService
import com.voximplant.demos.kotlin.video_call.services.VoximplantCallManager
import com.voximplant.sdk.hardware.ICameraManager

object Shared {
    lateinit var fileLogger: FileLogger
    lateinit var authService: AuthService
    lateinit var voximplantCallManager: VoximplantCallManager
    lateinit var foregroundCheck: ForegroundCheck
    lateinit var notificationHelper: NotificationHelper
    lateinit var cameraManager: ICameraManager
    lateinit var shareHelper: ShareHelper
}