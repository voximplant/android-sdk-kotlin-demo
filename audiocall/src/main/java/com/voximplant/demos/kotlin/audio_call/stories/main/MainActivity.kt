/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.main

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.databinding.ActivityMainBinding
import com.voximplant.demos.kotlin.audio_call.permissionsHelper
import com.voximplant.demos.kotlin.audio_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.audio_call.stories.login.LoginActivity
import com.voximplant.demos.kotlin.utils.*

class MainActivity : BaseActivity<MainViewModel>(MainViewModel::class.java) {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.model = model
        binding.lifecycleOwner = this

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser =
            AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        binding.callTo.setText(
            LAST_OUTGOING_CALL_USERNAME.getStringFromPrefs(applicationContext).orEmpty()
        )

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
            binding.callTo.text.toString()
                .saveToPrefs(applicationContext, LAST_OUTGOING_CALL_USERNAME)

            if (permissionsHelper.allPermissionsGranted()) {
                model.call(binding.callTo.text.toString())
            } else {
                ActivityCompat.requestPermissions(this, permissionsHelper.requiredPermissions, 1)
            }
        }

        model.moveToCall.observe(this, {
            Intent(this, CallActivity::class.java).also {
                it.putExtra(IS_OUTGOING_CALL, true)
                startActivity(it)
            }
        })

        model.moveToLogin.observe(this, {
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
            }
        })

        model.invalidInputError.observe(this, {
            showError(binding.callTo, resources.getString(it))
        })

        if (intent.getBooleanExtra(IS_ONGOING_CALL, false)) {
            Intent(this, CallActivity::class.java).also {
                it.putExtra(IS_ONGOING_CALL, true)
                it.putExtra(IS_INCOMING_CALL, false)
                it.putExtra(IS_OUTGOING_CALL, false)
                startActivity(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionsHelper.allPermissionsGranted = { model.call(binding.callTo.text.toString()) }
        permissionsHelper.permissionDenied =
            { _, openAppSettings ->
                Snackbar.make(
                    binding.root,
                    applicationContext.getString(R.string.permission_mic_to_call),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(applicationContext.getString(R.string.settings)) { openAppSettings() }
                    .show()
            }
    }

    override fun onBackPressed() {}

    private fun showError(textView: EditText, text: String) {
        textView.error = text
        textView.requestFocus()
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsHelper.permissionsResult(permissions, grantResults)
    }
}