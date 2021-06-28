package com.voximplant.demos.kotlin.videocall_deepar.utils

import android.annotation.SuppressLint
import com.voximplant.demos.kotlin.videocall_deepar.services.AuthService
import com.voximplant.demos.kotlin.videocall_deepar.services.CameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.services.DeepARHelper
import com.voximplant.demos.kotlin.videocall_deepar.services.VoximplantCallManager
import org.webrtc.EglBase


object Shared {
    lateinit var fileLogger: FileLogger
    lateinit var authService: AuthService
    lateinit var voximplantCallManager: VoximplantCallManager
    lateinit var foregroundCheck: ForegroundCheck
    lateinit var notificationHelper: NotificationHelper
    lateinit var shareHelper: ShareHelper

    @SuppressLint("StaticFieldLeak")
    lateinit var deepARHelper: DeepARHelper
    @SuppressLint("StaticFieldLeak")
    lateinit var cameraHelper: CameraHelper
    lateinit var eglBase: EglBase

}