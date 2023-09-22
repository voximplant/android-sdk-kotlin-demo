package com.voximplant.demos.kotlin.videocall_deepar.stories.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

        binding.callTo.setText(OUTGOING_USERNAME.getStringFromPrefs(applicationContext).orEmpty())

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
            binding.callTo.text.toString().saveToPrefs(applicationContext, OUTGOING_USERNAME)
            permissionsRequestCompletion = {
                model.call(binding.callTo.text.toString())
            }
            requestPermissions()
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
    }

    override fun onBackPressed() {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return  // no permission
                }
            }
            permissionsRequestCompletion?.invoke()
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                1
            )
        } else {
            // Permission has already been granted
            permissionsRequestCompletion?.invoke()
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    companion object {
        internal const val OUTGOING_USERNAME = "outgoing_username"
    }
}