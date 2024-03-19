/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.videocall_deepar.stories.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.voximplant.demos.kotlin.videocall_deepar.permissionsHelper
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.videocall_deepar.R
import com.voximplant.demos.kotlin.videocall_deepar.databinding.ActivityMainBinding
import com.voximplant.demos.kotlin.videocall_deepar.stories.call.CallActivity
import com.voximplant.demos.kotlin.videocall_deepar.stories.login.LoginActivity

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

        binding.callTo.editText?.setText(OUTGOING_USERNAME.getStringFromPrefs(applicationContext).orEmpty())

        binding.callTo.editText?.doOnTextChanged { _, _, _, _ ->
            showError(binding.callTo, null)
        }

        binding.startCallButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.logoutButton.setOnClickListener {
            model.logout()
        }

        binding.shareLogMainButton.setOnClickListener {
            Shared.shareHelper.shareLog(this)
        }

        binding.startCallButton.setOnClickListener {
            binding.callTo.editText?.text.toString().saveToPrefs(applicationContext, OUTGOING_USERNAME)
            if (permissionsHelper.allPermissionsGranted()) {
                model.call(binding.callTo.editText?.text.toString())
            } else {
                ActivityCompat.requestPermissions(this, permissionsHelper.requiredPermissions, 1)
            }
        }

        binding.presetCameraSwitch.setOnClickListener {
            model.toggleLocalVideoPreset()
        }

        model.displayName.observe(this) {
            binding.loggedInLabel.text = it
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

        model.localVideoPresetEnabled.observe(this) {
            binding.presetCameraSwitch.isChecked = it
        }

        model.callToFieldError.observe(this) { value ->
            showError(binding.callTo, resources.getString(value))
        }

        binding.callTo.editText?.doOnTextChanged { _, _, _, _ ->
            showError(binding.callTo, null)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.permissionsResult(permissions, grantResults)
    }

    companion object {
        internal const val OUTGOING_USERNAME = "outgoing_username"
    }
}
