package com.voximplant.demos.kotlin.videocall_deepar.stories.incoming_call

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.videocall_deepar.R
import com.voximplant.demos.kotlin.videocall_deepar.stories.call.CallActivity
import com.voximplant.demos.kotlin.videocall_deepar.stories.call_failed.CallFailedActivity
import com.voximplant.demos.kotlin.videocall_deepar.stories.main.MainActivity
import kotlinx.android.synthetic.main.activity_incoming_call.*

class IncomingCallActivity :
    BaseActivity<IncomingCallViewModel>(IncomingCallViewModel::class.java) {
    private var permissionsRequestCompletion: (() -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        answer_call_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        decline_call_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        answer_call_button.setOnClickListener {
            permissionsRequestCompletion = {
                model.answer()
            }
            requestPermissions()
        }

        decline_call_button.setOnClickListener {
            model.decline()
        }

        preset_camera_switch.setOnClickListener {
            model.toggleLocalVideoPreset()
        }

        model.moveToCall.observe(this, {
            Intent(this, CallActivity::class.java).also {
                it.putExtra(IS_INCOMING_CALL, true)
                it.putExtra(PRESET_SEND_LOCAL_VIDEO, model.localVideoPresetEnabled.value == true)
                startActivity(it)
            }
        })

        model.moveToCallFailed.observe(this, { reason ->
            Intent(this, CallFailedActivity::class.java).also {
                it.putExtra(FAIL_REASON, reason)
                it.putExtra(PRESET_SEND_LOCAL_VIDEO, intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true))
                startActivity(it)
            }
        })

        model.moveToMainActivity.observe(this, {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        })

        model.displayName.observe(this, {
            incoming_call_from.text = it
        })

        model.localVideoPresetEnabled.observe(this) {
            preset_camera_switch.isChecked = it;
        }

        val intent = intent
        val result = intent.getBooleanExtra(ACTION_ANSWER_INCOMING_CALL, false)
        if (result) {
            Shared.notificationHelper.cancelIncomingCallNotification()
            permissionsRequestCompletion = { model.answer() }
            requestPermissions()
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
            model.answer()
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
            model.answer()
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}