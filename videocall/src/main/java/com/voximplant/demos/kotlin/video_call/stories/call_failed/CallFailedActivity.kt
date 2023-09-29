package com.voximplant.demos.kotlin.video_call.stories.call_failed

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import com.voximplant.demos.kotlin.utils.*
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.databinding.ActivityCallFailedBinding
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity

class CallFailedActivity : BaseActivity<CallFailedViewModel>(CallFailedViewModel::class.java) {
    private lateinit var binding: ActivityCallFailedBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallFailedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.model = model

        model.setEndpoint(userName = intent.getStringExtra(ENDPOINT_USERNAME), displayName = intent.getStringExtra(ENDPOINT_DISPLAY_NAME))

        val failReason = intent.getStringExtra(FAIL_REASON)
        binding.callFailedStatus.text = failReason

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        binding.cancelCallButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.callBackButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.cancelCallButton.setOnClickListener {
            model.cancel()
        }

        binding.callBackButton.setOnClickListener {
            model.callBack(sendVideo = intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true))
        }

        model.displayName.observe(this) { value ->
            binding.callerNameCallFailed.text = value
        }

        model.moveToCall.observe(this) {
            Intent(this, CallActivity::class.java).apply {
                putExtra(IS_INCOMING_CALL, false)
                putExtra(PRESET_SEND_LOCAL_VIDEO, intent.getBooleanExtra(PRESET_SEND_LOCAL_VIDEO, true))
                startActivity(this)
            }
        }

        model.moveToMainActivity.observe(this) {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}
