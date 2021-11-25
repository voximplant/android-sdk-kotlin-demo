/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class NotificationHelper(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val appName: String,
) {
    private lateinit var incomingCallNotification: NotificationCompat.Builder
    lateinit var ongoingCallNotification: NotificationCompat.Builder
    var ongoingCallNotificationId: Int = INVALID_NOTIFICATION_ID
        private set

    init {
        makeChannelGroup()
    }

    private fun makeChannelGroup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                NOTIFICATION_CALLS_GROUP_ID,
                context.getString(R.string.notification_group_calls),
            )
        )
        Log.d(APP_TAG, "NotificationHelper::makeChannelGroup")
        makeIncomingCallChannel()
        makeOngoingCallChannel()
    }

    private fun makeIncomingCallChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (notificationManager.getNotificationChannel(INCOMING_CALL_CHANNEL_ID) != null) {
            return
        }
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        NotificationChannel(
            INCOMING_CALL_CHANNEL_ID,
            context.getString(R.string.notification_incoming_call),
            NotificationManager.IMPORTANCE_HIGH
        )
            .apply {
                group = NOTIFICATION_CALLS_GROUP_ID
                setSound(ringtoneUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000)
            }
            .also { notificationManager.createNotificationChannel(it) }
        Log.d(APP_TAG, "NotificationHelper::makeIncomingCallChannel")
    }

    private fun makeOngoingCallChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        if (notificationManager.getNotificationChannel(ONGOING_CALL_CHANNEL_ID) != null) {
            return
        }
        NotificationChannel(
            ONGOING_CALL_CHANNEL_ID,
            context.getString(R.string.notification_ongoing_call),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
            .apply { group = NOTIFICATION_CALLS_GROUP_ID }
            .also { notificationManager.createNotificationChannel(it) }
        Log.d(APP_TAG, "NotificationHelper::makeOngoingCallChannel")
    }

    fun showIncomingCallNotification(
        context: Context,
        intent: Intent,
        displayName: String,
    ) {
        val incomingCallPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent.putExtra(IS_INCOMING_CALL, true),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        val answerPendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent().setAction(ACTION_ANSWER_INCOMING_CALL),
                0
            )
        val declinePendingIntent =
            PendingIntent.getBroadcast(
                context,
                2,
                Intent().setAction(ACTION_DECLINE_INCOMING_CALL),
                0
            )
        incomingCallNotification =
            NotificationCompat.Builder(context, INCOMING_CALL_CHANNEL_ID).apply {
                setCategory(NotificationCompat.CATEGORY_CALL)
                priority = NotificationCompat.PRIORITY_HIGH
                setOngoing(true)
                setShowWhen(false)
                setSilent(Shared.appInForeground)
                setSmallIcon(R.drawable.ic_vox_notification)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getColor(R.color.colorPrimary)
                }
                setContentTitle(appName)
                setContentText("$displayName ${context.getString(R.string.username_is_calling)}")
                setContentIntent(incomingCallPendingIntent)
                if (!Shared.appInForeground) setFullScreenIntent(incomingCallPendingIntent, true)
                setVibrate(longArrayOf(0, 1000, 1000))
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioManager.STREAM_RING
                )
                addAction(
                    R.drawable.ic_baseline_call_24,
                    context.getString(R.string.answer),
                    answerPendingIntent
                )
                addAction(
                    R.drawable.ic_baseline_call_end_24,
                    context.getString(R.string.decline),
                    declinePendingIntent
                )
            }
        notificationManager.notify(
            incomingCallNotificationId,
            incomingCallNotification.build().also { it.flags += Notification.FLAG_INSISTENT })
        Log.d(APP_TAG, "NotificationHelper::showIncomingCallNotification id: $incomingCallNotificationId")
    }

    fun createOngoingCallNotification(
        context: Context,
        userName: String?,
        text: String?,
        cls: Class<*>,
    ) {
        ongoingCallNotificationId = Random.nextInt(from = 1, until = Int.MAX_VALUE)
        val intentOngoingCall = Intent(context, cls).let { intent ->
            intent.putExtra(IS_ONGOING_CALL, true)
            intent.putExtra(IS_OUTGOING_CALL, false)
            intent.putExtra(IS_INCOMING_CALL, false)
        }
        val ongoingCallPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intentOngoingCall,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        val hangupCallPendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent().setAction(ACTION_HANGUP_ONGOING_CALL),
                0
            )
        ongoingCallNotification =
            NotificationCompat.Builder(context, ONGOING_CALL_CHANNEL_ID).apply {
                setCategory(NotificationCompat.CATEGORY_CALL)
                setSilent(true)
                setOngoing(true)
                setUsesChronometer(true)
                setLocalOnly(true)
                setSmallIcon(R.drawable.ic_vox_notification)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    color = context.getColor(R.color.colorPrimary)
                }
                setContentTitle("${context.getString(R.string.ongoing)} $APP_TAG $appName")
                setContentText("$userName - $text")
                setContentIntent(ongoingCallPendingIntent)
                setFullScreenIntent(ongoingCallPendingIntent, false)
                priority = NotificationCompat.PRIORITY_LOW
                addAction(
                    R.drawable.ic_baseline_call_end_24,
                    context.getString(R.string.hangup),
                    hangupCallPendingIntent
                )
            }
        Log.d(APP_TAG, "NotificationHelper::showOngoingCallNotification id: $ongoingCallNotificationId")
    }

    fun updateOngoingNotification(userName: String?, callState: CallState, isOnHold: Boolean) {
        val hangupCallPendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                Intent().setAction(ACTION_HANGUP_ONGOING_CALL),
                0
            )
        if (ongoingCallNotificationId != INVALID_NOTIFICATION_ID) {
            ongoingCallNotification.apply {
                clearActions()
                addAction(
                    R.drawable.ic_baseline_call_end_24,
                    context.getString(R.string.hangup),
                    hangupCallPendingIntent
                )
                when {
                    callState == CallState.RECONNECTING -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            color = context.getColor(R.color.colorPrimary)
                            setColorized(false)
                        }
                        setContentText("$userName - $callState")
                    }
                    isOnHold -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            color = context.getColor(R.color.colorRed)
                            setColorized(true).setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        }
                        setContentText("$userName - ${context.getString(R.string.call_on_hold)}")
                    }
                    else -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            color = context.getColor(R.color.colorPrimary)
                            setColorized(false)
                        }
                        setContentText("$userName - ${context.getString(R.string.call_in_progress)}")
                    }
                }
            }

            notificationManager.notify(ongoingCallNotificationId, ongoingCallNotification.build())
            Log.d(APP_TAG, "NotificationHelper::updateOngoingNotification id: $ongoingCallNotificationId")
        }
    }

    fun cancelIncomingCallNotification() {
        notificationManager.cancel(incomingCallNotificationId)
        Log.d(APP_TAG, "NotificationHelper::cancelIncomingCallNotification id: $incomingCallNotificationId")
    }

    fun cancelOngoingCallNotification() {
        notificationManager.cancel(ongoingCallNotificationId)
        ongoingCallNotificationId = INVALID_NOTIFICATION_ID
        Log.d(APP_TAG, "NotificationHelper::cancelOngoingCallNotification id: $ongoingCallNotificationId")
    }

    companion object {
        private const val NOTIFICATION_CALLS_GROUP_ID = "VoximplantGroupCalls"
        private const val INCOMING_CALL_CHANNEL_ID = "VoximplantChannel_1_IncomingCall"
        private const val ONGOING_CALL_CHANNEL_ID = "VoximplantChannel_2_OngoingCall"
        private const val incomingCallNotificationId = 100
        private const val INVALID_NOTIFICATION_ID = 0
    }
}
