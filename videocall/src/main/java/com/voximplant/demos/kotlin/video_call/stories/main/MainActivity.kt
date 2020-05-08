package com.voximplant.demos.kotlin.video_call.stories.main

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Observer
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.utils.BaseActivity
import com.voximplant.demos.kotlin.video_call.utils.IS_INCOMING_CALL
import com.voximplant.sdk.Voximplant
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: BaseActivity<MainViewModel>(MainViewModel::class.java) {
    private var permissionsRequestCompletion: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        start_call_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        logout_button.setOnClickListener {
            model.logout()
        }

        start_call_button.setOnClickListener {
            permissionsRequestCompletion = {
                model.call(call_to.text.toString())
            }
            requestPermissions()
        }

        model.displayName.observe(this, Observer {
            logged_in_label.text = it
        })

        model.moveToCall.observe(this, Observer {
            Intent(this, CallActivity::class.java).also {
                it.putExtra(IS_INCOMING_CALL, false)
                startActivity(it)
            }
        })
    }

    override fun onBackPressed() { }

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
                permissionsRequestCompletion?.invoke()
            }
        }
    }

    private fun requestPermissions() {
        val missingPermissions =
            Voximplant.getMissingPermissions(applicationContext, true)
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