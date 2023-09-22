package com.voximplant.demos.kotlin.video_call.stories.incoming_call

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.databinding.ActivityIncomingCallBinding
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.stories.call_failed.CallFailedActivity

class IncomingCallActivity : BaseActivity<IncomingCallViewModel>(IncomingCallViewModel::class.java) {
    private lateinit var binding: ActivityIncomingCallBinding
    private var permissionsRequestCompletion: (() -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        binding.answerCallButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.declineCallButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.answerCallButton.setOnClickListener {
            permissionsRequestCompletion = {
                model.answer()
            }
            requestPermissions()
        }

        binding.declineCallButton.setOnClickListener {
            model.decline()
        }

        binding.presetCameraSwitch.setOnClickListener {
            model.toggleLocalVideoPreset()
        }

        model.moveToCall.observe(this) {
            Intent(this, CallActivity::class.java).apply {
                putExtra(IS_INCOMING_CALL, true)
                putExtra(PRESET_SEND_LOCAL_VIDEO, model.localVideoPresetEnabled.value == true)
                startActivity(this)
            }
        }

        model.moveToCallFailed.observe(this) { reason ->
            Intent(this, CallFailedActivity::class.java).apply {
                putExtra(FAIL_REASON, reason)
                putExtra(PRESET_SEND_LOCAL_VIDEO, intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true))
                startActivity(this)
            }
        }

        model.displayName.observe(this) { value ->
            binding.incomingCallFrom.text = value
        }

        model.localVideoPresetEnabled.observe(this) { value ->
            binding.presetCameraSwitch.isChecked = value
        }

        if (intent.getBooleanExtra(ACTION_ANSWER_INCOMING_CALL, false)) {
            Shared.notificationHelper.cancelIncomingCallNotification()
            permissionsRequestCompletion = { model.answer() }
            requestPermissions()
        }

        model.viewCreated()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            var audioGranted = false
            var videoGranted = false
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.RECORD_AUDIO && grantResults[i] == PermissionChecker.PERMISSION_GRANTED
                ) {
                    audioGranted = true
                }
                if (permissions[i] == Manifest.permission.CAMERA && grantResults[i] == PermissionChecker.PERMISSION_GRANTED
                ) {
                    videoGranted = true
                }
            }
            if (audioGranted && videoGranted) {
                model.answer()
            }
        }
    }

    private fun requestPermissions() {
        val missingPermissions =
            Voximplant.getMissingPermissions(applicationContext, true)
        // due to the bug in android 6.0:
        // https://stackoverflow.com/questions/32185628/connectivitymanager-requestnetwork-in-android-6-0
        if (Build.VERSION.SDK_INT == 23) {
            if (missingPermissions.contains(Manifest.permission.CHANGE_NETWORK_STATE)) {
                missingPermissions.remove(Manifest.permission.CHANGE_NETWORK_STATE)
            }
        }
        if (missingPermissions.isEmpty()) {
            permissionsRequestCompletion?.invoke()
        } else {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PermissionChecker.PERMISSION_GRANTED
            )
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}