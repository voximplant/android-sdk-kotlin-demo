package com.voximplant.demos.kotlin.videocall_deepar

import android.app.NotificationManager
import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.voximplant.demos.kotlin.videocall_deepar.services.*
import com.voximplant.demos.kotlin.videocall_deepar.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.client.ClientConfig
import org.webrtc.EglBase
import java.util.concurrent.Executors


class VideoCallApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        // Firebase
        FirebaseApp.initializeApp(applicationContext)

        // DeepARHelper
        Shared.deepARHelper = DeepARHelper(applicationContext)
        Shared.eglBase = EglBase.create()

        // CameraHelper
        Shared.cameraHelper = CameraHelper(applicationContext)

        // Voximplant
        val client = Voximplant.getClientInstance(
            Executors.newSingleThreadExecutor(),
            applicationContext,
            ClientConfig().also {
                it.packageName = packageName
                it.eglBase = Shared.eglBase
            },
        )
        Shared.authService = AuthService(client, Tokens(applicationContext), applicationContext)
        val notificationHelper =
            NotificationHelper(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also {
                Shared.notificationHelper = it
            }
        Shared.voximplantCallManager =
            VoximplantCallManager(client, applicationContext, notificationHelper)

        // Foreground
        Shared.foregroundCheck = ForegroundCheck().also {
            registerActivityLifecycleCallbacks(it)
        }

        // Logging
        Shared.fileLogger = FileLogger(this)
        Shared.shareHelper = ShareHelper.also { it.init(this) }
    }
}