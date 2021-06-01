package com.voximplant.demos.kotlin.video_call

import android.app.NotificationManager
import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.voximplant.demos.kotlin.video_call.services.AuthService
import com.voximplant.demos.kotlin.video_call.services.VoximplantCallManager
import com.voximplant.demos.kotlin.video_call.services.Tokens
import com.voximplant.demos.kotlin.video_call.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.client.ClientConfig
import java.util.concurrent.Executors

class VideoCallApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(applicationContext)

        val client = Voximplant.getClientInstance(
            Executors.newSingleThreadExecutor(),
            applicationContext,
            ClientConfig().also {
                it.packageName = packageName
            }
        )

        val notificationHelper = NotificationHelper(
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ).also {
            Shared.notificationHelper = it
        }
        Shared.fileLogger = FileLogger(this)
        Shared.authService = AuthService(client, Tokens(applicationContext), applicationContext)
        Shared.voximplantCallManager = VoximplantCallManager(client, applicationContext, notificationHelper)
        Shared.foregroundCheck = ForegroundCheck().also {
            registerActivityLifecycleCallbacks(it)
        }
        Shared.cameraManager = Voximplant.getCameraManager(applicationContext)
        Shared.shareHelper = ShareHelper.also { it.init(this) }
    }
}