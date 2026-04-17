package com.voximplant.demos.kotlin.services

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voximplant.demos.kotlin.utils.Shared

class FirebasePushService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val push = remoteMessage.data
        val intent = Intent(this, BackgroundPushService::class.java).apply {
            putExtra("push", Bundle().apply {
                for ((key, value) in push) {
                    putString(key, value)
                }
            })
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
    }

    override fun onNewToken(token: String) {
        Shared.authService.firebaseTokenRefreshed(token)
    }
}
