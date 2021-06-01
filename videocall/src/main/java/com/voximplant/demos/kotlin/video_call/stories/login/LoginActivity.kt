package com.voximplant.demos.kotlin.video_call.stories.login

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.video_call.utils.BaseActivity
import com.voximplant.demos.kotlin.video_call.utils.Shared
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity: BaseActivity<LoginViewModel>(LoginViewModel::class.java) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        loginButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        loginButton.setOnClickListener {
            model.login(usernameView.text.toString(), passwordView.text.toString())
        }

        shareLogLoginButton.setOnClickListener {
            Shared.shareHelper.shareLog(this)
        }

        model.didLogin.observe(this, {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        })

        model.invalidInputError.observe(this, {
            showError(
                when (it.first) {
                    true -> usernameView
                    false -> passwordView
                }
                , resources.getString(it.second)
            )
        })

        model.usernameFieldText.observe(this, {
            usernameView.setText(it)
        })

        model.passwordFieldText.observe(this, {
            passwordView.setText(it)
        })
    }

    private fun showError(textView: EditText, text: String) {
        textView.error = text
        textView.requestFocus()
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}