/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.video_call.stories.call

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.databinding.ActivityCallBinding
import com.voximplant.demos.kotlin.video_call.stories.call_failed.CallFailedActivity
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity
import com.voximplant.sdk.hardware.AudioDevice
import kotlinx.android.synthetic.main.activity_call.*
import org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL
import org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT

class CallActivity : BaseActivity<CallViewModel>(CallViewModel::class.java) {
    private lateinit var binding: ActivityCallBinding
    private var screenSharingRequestCompletion: ((Intent?) -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.model = model

        local_video_view.setZOrderMediaOverlay(true)

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        mute_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        audio_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        hold_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        sharing_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        video_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        hangup_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        mute_button.setOnClickListener {
            model.mute()
        }

        audio_button.setOnClickListener {
            showAudioDeviceSelectionDialog(model.availableAudioDevices)
        }

        hold_button.setOnClickListener {
            model.hold()
        }

        sharing_button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                model.shareScreen(::requestScreenCapture)
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.screen_sharing_min_api_warning),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        video_button.setOnClickListener {
            model.sendVideo()
        }

        hangup_button.setOnClickListener {
            model.hangup()
        }

        local_video_view.setOnClickListener {
            model.changeCam()
        }

        model.showLocalVideoView.observe(this, {
            local_video_view.visibility = if (it) VISIBLE else INVISIBLE
        })

        model.showRemoteVideoView.observe(this, {
            remote_video_view.visibility = if (it) VISIBLE else INVISIBLE
        })

        model.remoteVideoIsPortrait.observe(this, { isPortrait ->
            remote_video_view.setScalingType(if (isPortrait) SCALE_ASPECT_FILL else SCALE_ASPECT_FIT)
        })

        model.activeDevice.observe(this, { audioDevice ->
            when (audioDevice) {
                AudioDevice.EARPIECE -> audio_button_icon.setImageResource(R.drawable.ic_audio_internal)
                AudioDevice.SPEAKER -> audio_button_icon.setImageResource(R.drawable.ic_audio_external)
                AudioDevice.WIRED_HEADSET -> audio_button_icon.setImageResource(R.drawable.ic_audio_headphones)
                AudioDevice.BLUETOOTH -> audio_button_icon.setImageResource(R.drawable.ic_bluetooth)
                AudioDevice.NONE -> audio_button_icon.setImageResource(R.drawable.ic_audio_disabled)
                null -> audio_button_icon.setImageResource(R.drawable.ic_audio_disabled)
            }
        })

        model.muted.observe(this, { muted ->
            if (muted) {
                mute_button.setBackgroundResource(R.drawable.red_call_option_back)
                mute_button_icon.setImageResource(R.drawable.ic_micoff)
            } else {
                mute_button.setBackgroundResource(R.drawable.normal_call_option_back)
                mute_button_icon.setImageResource(R.drawable.ic_micon)
            }
        })

        model.onHold.observe(this, { onHold ->
            if (onHold) {
                hold_button.setBackgroundResource(R.drawable.red_call_option_back)
                hold_button_icon.setImageResource(R.drawable.ic_call_hold)
            } else {
                hold_button.setBackgroundResource(R.drawable.normal_call_option_back)
                hold_button_icon.setImageResource(R.drawable.ic_call_hold)
            }
        })

        model.sharingScreen.observe(this, { sharingScreen ->
            if (sharingScreen) {
                sharing_button.setBackgroundResource(R.drawable.red_call_option_back)
            } else {
                sharing_button.setBackgroundResource(R.drawable.normal_call_option_back)
            }
        })

        model.sendingVideo.observe(this, { sendingVideo ->
            if (sendingVideo) {
                video_button.setBackgroundResource(R.drawable.normal_call_option_back)
                video_button_icon.setImageResource(R.drawable.ic_camon)
            } else {
                video_button.setBackgroundResource(R.drawable.red_call_option_back)
                video_button_icon.setImageResource(R.drawable.ic_camoff)
                local_video_view.visibility = INVISIBLE
            }
        })

        Shared.voximplantCallManager.localVideoRenderer.observe(this, { completion ->
            completion(local_video_view)
        })

        Shared.voximplantCallManager.remoteVideoRenderer.observe(this, { completion ->
            completion(remote_video_view)
        })

        model.moveToCallFailed.observe(this, { reason ->
            Intent(this, CallFailedActivity::class.java).also {
                it.putExtra(FAIL_REASON, reason)
                startActivity(it)
            }
        })

        model.moveToMainActivity.observe(this, {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        })

        model.enableSharingButton.observe(this, {
            sharing_button.isEnabled = it
        })

        model.enableHoldButton.observe(this, {
            hold_button.isEnabled = it
        })

        model.enableVideoButton.observe(this, {
            video_button.isClickable = it
        })

        model.onCreateWithCall(
            intent.getBooleanExtra(IS_INCOMING_CALL, true),
            intent.getBooleanExtra(IS_ONGOING_CALL, false)
        )
    }

    override fun onBackPressed() {}

    private fun showAudioDeviceSelectionDialog(audioDevices: List<String>) {
        AlertDialog.Builder(this).setTitle(R.string.alert_select_audio_device)
            .setItems(audioDevices.toTypedArray()) { _, which ->
                model.selectAudioDevice(which)
            }
            .create()
            .show()
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK)
                screenSharingRequestCompletion?.invoke(result.data)
            else
                screenSharingRequestCompletion?.invoke(null)
        }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestScreenCapture(completion: (Intent?) -> Unit) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenSharingRequestCompletion = completion
        resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}