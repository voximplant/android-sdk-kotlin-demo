package com.voximplant.demos.kotlin.video_call.stories.call_failed

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Observer
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.call.CallActivity
import com.voximplant.demos.kotlin.video_call.utils.BaseActivity
import com.voximplant.demos.kotlin.video_call.utils.FAIL_REASON
import com.voximplant.demos.kotlin.video_call.utils.IS_INCOMING_CALL
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_call_failed.*

class CallFailedActivity: BaseActivity<CallFailedViewModel>(CallFailedViewModel::class.java) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_failed)

        val failReason = intent.getStringExtra(FAIL_REASON)
        call_failed_status.text = failReason

        val reducer = AnimatorInflater.loadAnimator(applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(applicationContext, R.animator.regain_size)

        cancel_call_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        call_back_button.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        cancel_call_button.setOnClickListener {
            model.cancel()
        }

        call_back_button.setOnClickListener {
            model.callBack()
        }

        model.displayName.observe(this, Observer {
            caller_name_call_failed.text = it
        })

        model.moveToCall.observe(this, Observer {
            Intent(this, CallActivity::class.java).also {
                it.putExtra(IS_INCOMING_CALL, false)
                startActivity(it)
            }
        })
    }

    override fun onBackPressed() { }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}