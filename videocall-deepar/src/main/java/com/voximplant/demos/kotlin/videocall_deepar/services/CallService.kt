package com.voximplant.demos.kotlin.videocall_deepar.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.voximplant.demos.kotlin.videocall_deepar.utils.*
import kotlin.math.min

class CallService: Service(), SensorEventListener {
    private val notificationHelper: NotificationHelper = Shared.notificationHelper
    private var proximityWakelock: PowerManager.WakeLock? = null
    private var isProximityNear = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action ?: return START_NOT_STICKY

        if (action == ACTION_FOREGROUND_SERVICE_START) {
            startForeground(
                10,
                notificationHelper.buildForegroundServiceNotification(
                    applicationContext,
                    "Call in progress"
                )
            )
            (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.let { sensorManager ->
                val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) ?: return@let
                val powerManager =
                    getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return@let
                try {
                    if (proximityWakelock == null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            proximityWakelock = powerManager.newWakeLock(
                                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                                "VideoCall:voximplant-videocall-demo-proximity"
                            )
                        }
                    }
                    sensorManager.registerListener(
                        this,
                        proximity,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                } catch (e: Exception) {
                    Log.e(
                        APP_TAG,
                        "CallService: onStartCommand: exception on proximity sensor initialization: ${e.message}"
                    )
                }
            }

        } else if (action == ACTION_FOREGROUND_SERVICE_STOP) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onSensorChanged(sensorEvent: SensorEvent) {
        if (sensorEvent.sensor.type != Sensor.TYPE_PROXIMITY) { return }

        val newIsNear = sensorEvent.values.first() < min(sensorEvent.sensor.maximumRange,3f)
        if (newIsNear == isProximityNear) { return }
        isProximityNear = newIsNear

        proximityWakelock?.also {
            try {
                if (isProximityNear && !it.isHeld) {
                    Log.i(APP_TAG, "CallService: onSensorChanged: acquire wake lock")
                    it.acquire(1 * 60 * 1000L /*10 minutes*/)
                } else if (it.isHeld) {
                    Log.i(APP_TAG, "CallService: onSensorChanged: release wake lock")
                    it.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                }
            } catch (e: Exception) {
                Log.e(APP_TAG, "CallService: onSensorChanged: exception on proximity sensor: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.unregisterListener(this)
        proximityWakelock?.also {
            if (it.isHeld) {
                Log.i(APP_TAG, "CallService: onDestroy: release wake lock")
                it.release()
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}
