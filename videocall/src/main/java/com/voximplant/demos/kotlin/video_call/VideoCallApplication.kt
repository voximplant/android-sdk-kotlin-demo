package com.voximplant.demos.kotlin.video_call

import android.app.NotificationManager
import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.services.VoximplantCallManager
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.stories.incoming_call.IncomingCallActivity
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.VideoFlags
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

        Shared.notificationHelper =
            NotificationHelper(
                applicationContext,
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager,
                getString(R.string.app_name),
            )
        Shared.fileLogger = FileLogger(this)
        Shared.authService = AuthService(client, applicationContext)
        Shared.voximplantCallManager = VoximplantCallManager(
            applicationContext,
            client,
            VideoFlags(true, true),
            CallActivity::class.java,
            IncomingCallActivity::class.java,
        )
        Shared.cameraManager = Voximplant.getCameraManager(applicationContext)
        Shared.shareHelper = ShareHelper.also {
            it.init(
                this,
                "com.voximplant.demos.kotlin.video_call.fileprovider",
            )
        }
        Shared.getResource = GetResource(applicationContext)
    }
}