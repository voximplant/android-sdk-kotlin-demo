package com.voximplant.demos.kotlin.videocall_deepar.utils

import android.content.Context
import android.util.Log
import com.voximplant.sdk.Voximplant
import com.voximplant.sdk.client.ILogListener
import com.voximplant.sdk.client.LogLevel
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class FileLogger(context: Context) : ILogListener {
    private var fileOutputStream: FileOutputStream? = null
    private var dateFormat: DateFormat =
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS", Locale.getDefault())

    init {
        try {
            fileOutputStream = context.openFileOutput(FILE_NAME, Context.MODE_APPEND)
            Voximplant.setLogListener(this)
        } catch (e: FileNotFoundException) {
            Log.e(APP_TAG, "FileLogger: failed to open file")
        }
    }

    companion object {
        private const val FILE_NAME = "vox_log.txt"
    }

    override fun onLogMessage(level: LogLevel, log: String) {
        var logMessage = log
        try {
            logMessage = when (level) {
                LogLevel.ERROR -> "${dateFormat.format(Date())} ERROR:   $logMessage\n"
                LogLevel.WARNING -> "${dateFormat.format(Date())} WARNING: $logMessage\n"
                LogLevel.INFO -> "${dateFormat.format(Date())} INFO:    $logMessage\n"
                LogLevel.DEBUG -> "${dateFormat.format(Date())} DEBUG:   $logMessage\n"
                LogLevel.VERBOSE -> "${dateFormat.format(Date())} VERBOSE: $logMessage\n"
                else -> "${dateFormat.format(Date())} VERBOSE: $logMessage\n"
            }
            fileOutputStream?.write(logMessage.toByteArray())
        } catch (e: IOException) {
            Log.e(APP_TAG, "FileLogger: failed to write log message")
        }
    }
}