package com.voximplant.demos.kotlin.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voximplant.demos.kotlin.utils.Shared

class FirebasePushService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val push = remoteMessage.data
        if (push.containsKey("voximplant")) {
            Shared.authService.pushNotificationReceived(push)
        }
    }

    override fun onNewToken(token: String) {
        Shared.authService.firebaseTokenRefreshed(token)
    }
}
