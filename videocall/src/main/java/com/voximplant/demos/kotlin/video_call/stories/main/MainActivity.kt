/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.databinding.ActivityMainBinding
import com.voximplant.demos.kotlin.video_call.permissionsHelper
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.stories.login.LoginActivity
import java.lang.reflect.Method

class MainActivity : BaseActivity<MainViewModel>(MainViewModel::class.java) {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        binding.callTo.editText?.setText(LAST_OUTGOING_CALL_USERNAME.getStringFromPrefs(applicationContext).orEmpty())

        binding.startCallButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        ActivityCompat.requestPermissions(this, permissionsHelper.requiredPermissions, 1)

        if ("xiaomi" == Build.MANUFACTURER.lowercase()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isCustomPermissionGranted(OP_BACKGROUND_START_ACTIVITY) || !isCustomPermissionGranted(OP_SHOW_WHEN_LOCKED)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.allow_notifications_on_lock_screen_dialog_title))
                        .setMessage(getString(R.string.allow_notifications_on_lock_screen_dialog_message))
                        .setPositiveButton(getString(R.string.settings)) { _, _ ->
                            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                            intent.setClassName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.permissions.PermissionsEditorActivity"
                            )
                            intent.putExtra("extra_pkgname", packageName)
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.d("Voximplant", e.message.toString())
                            }
                        }
                        .setNegativeButton(getString(R.string.not_now)) { _, _ -> }
                        .setCancelable(false)
                        .show()
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.allow_notifications_on_lock_screen_dialog_title))
                        .setMessage(getString(R.string.allow_notifications_on_lock_screen_dialog_message))
                        .setNegativeButton(getString(R.string.not_now)) { _, _ -> }
                        .setPositiveButton(getString(R.string.settings)) { _, _ ->
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.d("Voximplant", e.message.toString())
                            }
                        }
                        .show()
                }
            }
        }

        binding.logoutButton.setOnClickListener {
            model.logout()
        }

        binding.shareLogMainButton.setOnClickListener {
            Shared.shareHelper.shareLog(this)
        }

        binding.startCallButton.setOnClickListener {
            binding.callTo.editText?.text.toString().saveToPrefs(applicationContext, LAST_OUTGOING_CALL_USERNAME)
            if (permissionsHelper.allPermissionsGranted()) {
                model.call(binding.callTo.editText?.text.toString())
            }
        }

        binding.presetCameraSwitch.setOnClickListener {
            model.toggleLocalVideoPreset()
        }

        model.displayName.observe(this) { value ->
            binding.loggedInLabel.text = value
        }

        model.moveToCall.observe(this) {
            Intent(this, CallActivity::class.java).apply {
                putExtra(IS_INCOMING_CALL, false)
                putExtra(PRESET_SEND_LOCAL_VIDEO, model.localVideoPresetEnabled.value == true)
                startActivity(this)
            }
        }

        model.moveToLogin.observe(this) {
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
            }
        }

        model.callToFieldError.observe(this) { value ->
            showError(binding.callTo, resources.getString(value))
        }

        binding.callTo.editText?.doOnTextChanged { _, _, _, _ ->
            showError(binding.callTo, null)
        }

        model.localVideoPresetEnabled.observe(this) { value ->
            binding.presetCameraSwitch.isChecked = value
        }
    }

    private fun isCustomPermissionGranted(permission: Int): Boolean {
        return try {
            val mgr = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val m: Method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            m.invoke(
                mgr,
                permission,
                Process.myUid(),
                this.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            true
        }
    }

    private fun showError(textView: TextInputLayout, text: String?) {
        textView.error = text
        textView.isErrorEnabled = text != null
        if (text != null) {
            textView.requestFocus()
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    override fun onStart() {
        super.onStart()
        permissionsHelper.allPermissionsGranted = { model.call(binding.callTo.editText?.text.toString()) }
        permissionsHelper.permissionDenied = { permission, openAppSettings ->
            var message: String? = null
            if (permission == Manifest.permission.RECORD_AUDIO) {
                message = applicationContext.getString(R.string.permission_mic_to_call)
            } else if (permission == Manifest.permission.BLUETOOTH_CONNECT) {
                message = applicationContext.getString(R.string.permission_bluetooth_to_call)
            }
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).setAction(applicationContext.getString(R.string.settings)) { openAppSettings() }.show()
            }
        }
    }

    companion object {

        private const val OP_BACKGROUND_START_ACTIVITY = 10021
        private const val OP_SHOW_WHEN_LOCKED = 10020
    }
}
