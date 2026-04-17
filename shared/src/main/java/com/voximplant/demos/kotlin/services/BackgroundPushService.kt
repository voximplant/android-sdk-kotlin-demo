package com.voximplant.demos.kotlin.services


import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.voximplant.demos.kotlin.utils.APP_TAG
import com.voximplant.demos.kotlin.utils.Shared


class BackgroundPushService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = Shared.notificationHelper.createBackgroundPushNotification()

        val restoredMap = mutableMapOf<String, String>()
        val data = intent?.getBundleExtra("push")

        if (data != null) {
            val keys = data.keySet()

            for (key in keys) {
                val value = data.getString(key)
                if (value != null) {
                    restoredMap[key] = value
                }
            }
        }

        try {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                } else {
                    0
                },
            )
        } catch (exception: Exception) {
            Log.d(APP_TAG, "BackgroundPushService::exception: $exception")
        }

        if (restoredMap.containsKey("voximplant")) {
            Shared.authService.pushNotificationReceived(restoredMap.toMap())
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
