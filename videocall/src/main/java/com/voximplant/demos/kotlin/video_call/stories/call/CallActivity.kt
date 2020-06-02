package com.voximplant.demos.kotlin.video_call.stories.call

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.call_failed.CallFailedActivity
import com.voximplant.demos.kotlin.video_call.utils.BaseActivity
import com.voximplant.demos.kotlin.video_call.utils.FAIL_REASON
import com.voximplant.demos.kotlin.video_call.utils.IS_INCOMING_CALL
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : BaseActivity<CallViewModel>(CallViewModel::class.java) {

    private var screenSharingRequestCompletion: ((Intent?) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

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
            model.shareScreen(::requestScreenCapture)
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

        model.muted.observe(this, Observer { muted ->
            if (muted) {
                card_mute.background = resources.getDrawable(R.drawable.red_call_option_back)
                mute_button.setImageDrawable(resources.getDrawable(R.drawable.ic_micoff))
            } else {
                card_mute.background = resources.getDrawable(R.drawable.normal_call_option_back)
                mute_button.setImageDrawable(resources.getDrawable(R.drawable.ic_micon))
            }
        })

        model.onHold.observe(this, Observer { onHold ->
            if (onHold) {
                local_video_view.visibility = INVISIBLE
                card_hold.background = resources.getDrawable(R.drawable.red_call_option_back)
                hold_button.setImageDrawable(resources.getDrawable(R.drawable.ic_call_hold))
            } else {
                local_video_view.visibility = VISIBLE
                card_hold.background = resources.getDrawable(R.drawable.normal_call_option_back)
                hold_button.setImageDrawable(resources.getDrawable(R.drawable.ic_call_hold))
            }
        })

        model.sharingScreen.observe(this, Observer { sharingScreen ->
            if (sharingScreen) {
                card_sharing.background = resources.getDrawable(R.drawable.red_call_option_back)
            } else {
                card_sharing.background = resources.getDrawable(R.drawable.normal_call_option_back  )
            }
        })

        model.sendingVideo.observe(this, Observer { sendingVideo ->
            if (sendingVideo) {
                card_video.background = resources.getDrawable(R.drawable.normal_call_option_back)
                video_button.setImageDrawable(resources.getDrawable(R.drawable.ic_camon))
            } else {
                card_video.background = resources.getDrawable(R.drawable.red_call_option_back)
                video_button.setImageDrawable(resources.getDrawable(R.drawable.ic_camoff))
            }
        })

        model.localVideoStreamAdded.observe(this, Observer { completion ->
            completion(local_video_view)
            local_video_view.visibility = VISIBLE
        })

        model.localVideoStreamRemoved.observe(this, Observer {
            local_video_view.visibility = INVISIBLE
        })

        model.remoteVideoStreamAdded.observe(this, Observer { completion ->
            completion(remote_video_view)
            remote_video_view.visibility = VISIBLE
        })

        model.remoteVideoStreamRemoved.observe(this, Observer {
            remote_video_view.visibility = INVISIBLE
        })

        model.moveToCallFailed.observe(this, Observer { reason ->
            Intent(this, CallFailedActivity::class.java).also {
                it.putExtra(FAIL_REASON, reason)
                startActivity(it)
            }
        })

        model.enableSharingButton.observe(this, Observer {
            sharing_button.isEnabled = it
        })

        model.enableHoldButton.observe(this, Observer {
            hold_button.isEnabled = it
        })
        model.enableVideoButton.observe(this, Observer {
            video_button.isEnabled = it
        })

        model.onCreateWithCall(intent.getBooleanExtra(IS_INCOMING_CALL, true))
    }

    override fun onBackPressed() { }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == screenSharingRequestCode) {
            screenSharingRequestCompletion?.invoke(data)
            screenSharingRequestCompletion = null
        }
    }

    private fun showAudioDeviceSelectionDialog(audioDevices: List<String>) {
        AlertDialog.Builder(this).setTitle(R.string.alert_select_audio_device)
            .setItems(audioDevices.toTypedArray()) { _, which ->
                model.selectAudioDevice(audioDevices[which])
            }
            .create()
            .show()
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestScreenCapture(completion: (Intent?) -> Unit) {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenSharingRequestCompletion = completion
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            screenSharingRequestCode
        )
    }

    private companion object {
        const val screenSharingRequestCode = 1
    }
}