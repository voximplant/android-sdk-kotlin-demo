/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity

class PermissionsHelper(private val context: Context, val requiredPermissions: Array<String>) {

    var allPermissionsGranted: (() -> Unit)? = null
    var permissionDenied: ((permission: String, openAppSettings: () -> Unit) -> Unit)? = null

    fun permissionsResult(
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty()) {
            var allGranted = true
            for ((index, permission) in permissions.withIndex()) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    Log.w(APP_TAG, "$permission permission denied")
                    allGranted = false
                    permissionDenied?.invoke(permission, ::openAppSettings)
                    break
                }
            }
            if (allGranted)
                allPermissionsGranted?.invoke()
        } else {
            allPermissionsGranted?.invoke()
        }
    }

    fun allPermissionsGranted(): Boolean {
        var allPermissionsGranted = true
        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            )
                allPermissionsGranted = false
        }
        return allPermissionsGranted
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(context, intent, null)
    }
}