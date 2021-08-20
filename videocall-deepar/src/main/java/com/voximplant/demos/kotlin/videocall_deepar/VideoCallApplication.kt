package com.voximplant.demos.kotlin.videocall_deepar

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.services.VoximplantCallManager
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.videocall_deepar.services.CameraHelper
import com.voximplant.demos.kotlin.videocall_deepar.services.DeepARHelper
import com.voximplant.demos.kotlin.videocall_deepar.stories.call.CallActivity
import com.voximplant.demos.kotlin.videocall_deepar.stories.incoming_call.IncomingCallActivity
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.VideoFlags
import com.voximplant.sdk.client.ClientConfig
import org.webrtc.EglBase
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
lateinit var deepARHelper: DeepARHelper

@SuppressLint("StaticFieldLeak")
lateinit var cameraHelper: CameraHelper

class VideoCallApplication : MultiDexApplication(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        // Firebase
        //FirebaseApp.initializeApp(applicationContext)

        deepARHelper = DeepARHelper(applicationContext)
        Shared.eglBase = EglBase.create()
        cameraHelper = CameraHelper(applicationContext)

        // Voximplant
        val client = Voximplant.getClientInstance(
            Executors.newSingleThreadExecutor(),
            applicationContext,
            ClientConfig().also {
                it.packageName = packageName
                it.eglBase = Shared.eglBase
            },
        )

        Shared.authService = AuthService(client, applicationContext)
        Shared.notificationHelper =
            NotificationHelper(
                applicationContext,
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager,
                getString(R.string.app_name),
            ).also {
                Shared.notificationHelper = it
            }
        Shared.voximplantCallManager =
            VoximplantCallManager(
                applicationContext,
                client,
                VideoFlags(true, true),
                CallActivity::class.java,
                IncomingCallActivity::class.java,
            )

        // Logging
        Shared.fileLogger = FileLogger(this)
        Shared.shareHelper = ShareHelper.also {
            it.init(
                this,
                "com.voximplant.demos.kotlin.videocall_deepar.fileprovider"
            )
        }
        Shared.getResource = GetResource(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Shared.appInForeground = false
        Log.d(APP_TAG, "VideoCallApplication::onAppBackgrounded")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Shared.appInForeground = true
        Log.d(APP_TAG, "VideoCallApplication::onAppForegrounded")
    }
}