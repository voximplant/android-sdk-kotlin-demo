package com.voximplant.demos.kotlin.video_call.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File


object ShareHelper {
    private var fileUri: Uri? = null

    fun shareLog(context: Context) {
        if (fileUri != null) {
            ShareCompat.IntentBuilder(context)
                .setStream(fileUri)
                .setType("text/*")
                .startChooser()
        }
    }

    // Initialize it first on application startup (before calling shareLog)
    fun init(context: Context) {
        val logFile = File(context.filesDir.toString() + "/vox_log.txt")
        fileUri = try {
            FileProvider.getUriForFile(
                context,
                "com.voximplant.demos.kotlin.video_call.fileprovider",
                logFile
            )
        } catch (e: IllegalArgumentException) {
            Log.e(APP_TAG, "Selected file can't be shared")
            null
        }
    }
}