/*
 * Copyright (c) 2011 - 2025, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.voximplant.demos.kotlin.audio_call.services.AudioCallManager
import com.voximplant.demos.kotlin.audio_call.services.AudioCallManagerDefault
import com.voximplant.demos.kotlin.audio_call.services.AudioCallManagerTelecom
import com.voximplant.demos.kotlin.services.AuthService
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.client.ClientConfig
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
lateinit var permissionsHelper: PermissionsHelper

@SuppressLint("StaticFieldLeak")
lateinit var audioCallManager: AudioCallManager

class AudioCallApplication : MultiDexApplication(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(applicationContext)

        val client = Voximplant.getClientInstance(
            Executors.newSingleThreadExecutor(),
            applicationContext,
            ClientConfig().also { it.packageName = packageName },
        )

        Shared.notificationHelper =
            NotificationHelper(
                applicationContext,
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager,
                getString(R.string.app_name),
            )
        Shared.fileLogger = FileLogger(this)
        Shared.authService = AuthService(client, applicationContext)
        audioCallManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM) || applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CONNECTION_SERVICE))) {
            AudioCallManagerTelecom(applicationContext, client)
        } else {
            AudioCallManagerDefault(applicationContext, client)
        }

        val requiredPermissions: Set<String> = buildSet {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.MANAGE_OWN_CALLS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (audioCallManager !is AudioCallManagerTelecom) { // BLUETOOTH_CONNECT permission is not required while using Telecom Connection.
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        permissionsHelper = PermissionsHelper(applicationContext, requiredPermissions.toTypedArray())

        Shared.shareHelper = ShareHelper.also {
            it.init(
                this,
                "com.voximplant.demos.kotlin.audio_call.fileprovider",
            )
        }
        Shared.getResource = GetResource(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Shared.appInForeground = false
        Log.d(APP_TAG, "AudioCallApplication::onAppBackgrounded")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Shared.appInForeground = true
        Log.d(APP_TAG, "AudioCallApplication::onAppForegrounded")
    }
}