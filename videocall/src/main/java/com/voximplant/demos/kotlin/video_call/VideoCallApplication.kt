package com.voximplant.demos.kotlin.video_call

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Build
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
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.stories.incoming_call.IncomingCallActivity
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.call.VideoFlags
import com.voximplant.sdk.client.ClientConfig
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
lateinit var permissionsHelper: PermissionsHelper

class VideoCallApplication : MultiDexApplication(), LifecycleObserver {
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

        val requiredPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            }

        permissionsHelper = PermissionsHelper(applicationContext, requiredPermissions)

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