/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
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
import com.voximplant.demos.kotlin.utils.Shared.voximplantCallManager
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.databinding.ActivityCallBinding
import com.voximplant.demos.kotlin.video_call.stories.call_failed.CallFailedActivity
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity
import com.voximplant.sdk.hardware.AudioDevice
import com.voximplant.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL
import com.voximplant.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT

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

        binding.localVideoView.setZOrderMediaOverlay(true)

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        binding.muteButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.audioButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.holdButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.sharingButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.videoButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.hangupButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.muteButton.setOnClickListener {
            model.mute()
        }

        binding.audioButton.setOnClickListener {
            showAudioDeviceSelectionDialog(model.availableAudioDevices)
        }

        binding.holdButton.setOnClickListener {
            model.hold()
        }

        binding.sharingButton.setOnClickListener {
            model.shareScreen(::requestScreenCapture)
        }

        binding.videoButton.setOnClickListener {
            model.sendVideo()
        }

        binding.hangupButton.setOnClickListener {
            model.hangup()
        }

        binding.localVideoView.setOnClickListener {
            model.changeCam()
        }

        model.showLocalVideoView.observe(this) { value ->
            binding.localVideoView.visibility = if (value) VISIBLE else INVISIBLE
        }

        model.showRemoteVideoView.observe(this) { value ->
            binding.remoteVideoView.visibility = if (value) VISIBLE else INVISIBLE
        }

        model.remoteVideoIsPortrait.observe(this) { isPortrait ->
            binding.remoteVideoView.setScalingType(if (isPortrait) SCALE_ASPECT_FILL else SCALE_ASPECT_FIT)
        }

        model.activeDevice.observe(this) { audioDevice ->
            when (audioDevice) {
                AudioDevice.EARPIECE -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_internal)
                AudioDevice.SPEAKER -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_external)
                AudioDevice.WIRED_HEADSET -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_headphones)
                AudioDevice.BLUETOOTH -> binding.audioButtonIcon.setImageResource(R.drawable.ic_bluetooth)
                AudioDevice.NONE -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_disabled)
                null -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_disabled)
            }
        }

        model.muted.observe(this) { muted ->
            if (muted) {
                binding.muteButton.setBackgroundResource(R.drawable.red_call_option_back)
                binding.muteButtonIcon.setImageResource(R.drawable.ic_micoff)
            } else {
                binding.muteButton.setBackgroundResource(R.drawable.normal_call_option_back)
                binding.muteButtonIcon.setImageResource(R.drawable.ic_micon)
            }
        }

        model.onHold.observe(this) { onHold ->
            if (onHold) {
                binding.holdButton.setBackgroundResource(R.drawable.red_call_option_back)
                binding.holdButtonIcon.setImageResource(R.drawable.ic_call_hold)
            } else {
                binding.holdButton.setBackgroundResource(R.drawable.normal_call_option_back)
                binding.holdButtonIcon.setImageResource(R.drawable.ic_call_hold)
            }
        }

        model.sharingScreen.observe(this) { sharingScreen ->
            if (sharingScreen) {
                binding.sharingButton.setBackgroundResource(R.drawable.red_call_option_back)
            } else {
                binding.sharingButton.setBackgroundResource(R.drawable.normal_call_option_back)
            }
        }

        model.sendingLocalVideo.observe(this) { sendingVideo ->
            if (sendingVideo) {
                binding.videoButton.setBackgroundResource(R.drawable.normal_call_option_back)
                binding.videoButtonIcon.setImageResource(R.drawable.ic_camon)
            } else {
                binding.videoButton.setBackgroundResource(R.drawable.red_call_option_back)
                binding.videoButtonIcon.setImageResource(R.drawable.ic_camoff)
                binding.localVideoView.visibility = INVISIBLE
            }
        }

        voximplantCallManager.localVideoRenderer.observe(this) { completion ->
            completion(binding.localVideoView)
        }

        voximplantCallManager.remoteVideoRenderer.observe(this) { completion ->
            completion(binding.remoteVideoView)
        }

        model.moveToCallFailed.observe(this) { reason ->
            Intent(this, CallFailedActivity::class.java).apply {
                putExtra(ENDPOINT_USERNAME, model.userName.value)
                putExtra(ENDPOINT_DISPLAY_NAME, model.displayName.value)
                putExtra(FAIL_REASON, reason)
                putExtra(PRESET_SEND_LOCAL_VIDEO, intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true))
                startActivity(this)
            }
        }

        model.moveToMainActivity.observe(this) {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        }

        model.enableSharingButton.observe(this) { value ->
            binding.sharingButton.isEnabled = value
        }

        model.enableHoldButton.observe(this) { value ->
            binding.holdButton.isEnabled = value
        }

        model.enableVideoButton.observe(this) { value ->
            binding.videoButton.isClickable = value
        }

        model.onCreateWithCall(
            intent.getBooleanExtra(IS_INCOMING_CALL, true),
            intent.getBooleanExtra(IS_ONGOING_CALL, false),
            sendVideo = intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true),
        )
    }

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

    private fun requestScreenCapture(completion: (Intent?) -> Unit) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenSharingRequestCompletion = completion
        resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
