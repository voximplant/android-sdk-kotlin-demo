/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.login

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.voximplant.demos.kotlin.utils.BaseActivity
import com.voximplant.demos.kotlin.utils.Shared
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.databinding.ActivityLoginBinding
import com.voximplant.demos.kotlin.audio_call.stories.main.MainActivity

class LoginActivity : BaseActivity<LoginViewModel>(LoginViewModel::class.java) {
    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        binding.loginButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.loginButton.setOnClickListener {
            model.login(binding.usernameView.text.toString(), binding.passwordView.text.toString())
        }

        binding.shareLogLoginButton.setOnClickListener {
            Shared.shareHelper.shareLog(this)
        }

        model.didLogin.observe(this) {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        }

        model.invalidInputError.observe(this) {
            showError(
                when (it.first) {
                    true -> binding.usernameView
                    false -> binding.passwordView
                }, resources.getString(it.second)
            )
        }

        model.usernameFieldText.observe(this) { value ->
            binding.usernameView.setText(value)
        }

        model.passwordFieldText.observe(this) { value ->
            binding.passwordView.setText(value)
        }
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