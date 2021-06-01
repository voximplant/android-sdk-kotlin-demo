package com.voximplant.demos.kotlin.video_call.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity

class NotificationHelper(private val notificationManager: NotificationManager) {
    init {
        makeForegroundServiceChannel()
    }

    private fun makeIncomingCallChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (notificationManager.getNotificationChannel(INCOMING_CALL_CHANNEL_ID) != null) {
            return
        }
        NotificationChannel(
            INCOMING_CALL_CHANNEL_ID,
            INCOMING_CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
            .apply { description = INCOMING_CALL_CHANNEL_INFO }
            .also { notificationManager.createNotificationChannel(it) }
    }

    private fun makeForegroundServiceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (notificationManager.getNotificationChannel(FOREGROUND_SERVICE_CHANNEL_ID) != null) {
            return
        }
        NotificationChannel(
            FOREGROUND_SERVICE_CHANNEL_ID,
            FOREGROUND_SERVICE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
            .apply { description = FOREGROUND_SERVICE_CHANNEL_INFO }
            .also { notificationManager.createNotificationChannel(it) }
    }

    fun showCallNotification(
        context: Context,
        intent: Intent,
        displayName: String
    ) {
        makeIncomingCallChannel()

        val mainActionPendingIntent = PendingIntent.getActivity(
            context, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent.putExtra(CALL_ANSWERED, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent().setAction(ACTION_DECLINE_CALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT
        )

        notificationManager.notify(
            incomingCallNotificationId,
            NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_vox_notification)
                .setContentTitle(APP_TAG)
                .setContentText("$displayName is calling")
                .setFullScreenIntent(mainActionPendingIntent, true)
                .addAction(
                    R.drawable.ic_vox_notification,
                    "Answer",
                    answerPendingIntent
                )
                .addAction(
                    R.drawable.ic_vox_notification,
                    "Decline",
                    declinePendingIntent
                )
                .build()
        )
    }

    fun buildForegroundServiceNotification(
        context: Context?,
        text: String?
    ): Notification {
        val intentCall = Intent(context, CallActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(IS_STARTED_CALL, true)
        }

        return NotificationCompat.Builder(context!!, FOREGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vox_notification)
            .setContentTitle(APP_TAG)
            .setContentText(text)
            .setFullScreenIntent(PendingIntent.getActivity(
                context, 0,
                intentCall, 0
            ), true)
            .build()
    }

    fun cancelNotification() {
        notificationManager.cancel(incomingCallNotificationId)
    }

    companion object {
        private const val INCOMING_CALL_CHANNEL_ID = "VoximplantChannelIncomingCalls"
        private const val FOREGROUND_SERVICE_CHANNEL_ID = "VoximplantCallServiceChannel"
        private const val INCOMING_CALL_CHANNEL_NAME = "CallChannel"
        private const val INCOMING_CALL_CHANNEL_INFO = "Audio calls notifications"
        private const val FOREGROUND_SERVICE_CHANNEL_INFO = "Call service notifications"
        private const val FOREGROUND_SERVICE_CHANNEL_NAME = "ChannelCallService"
        private const val incomingCallNotificationId = 100
    }
}
