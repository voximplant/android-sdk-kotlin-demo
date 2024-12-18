/*
 * Copyright (c) 2011 - 2024, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.videocall_deepar.stories.login

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.voximplant.demos.kotlin.utils.BaseActivity
import com.voximplant.demos.kotlin.utils.Shared
import com.voximplant.demos.kotlin.videocall_deepar.R
import com.voximplant.demos.kotlin.videocall_deepar.databinding.ActivityLoginBinding
import com.voximplant.demos.kotlin.videocall_deepar.stories.main.MainActivity
import com.voximplant.sdk.client.Node

class LoginActivity : BaseActivity<LoginViewModel>(LoginViewModel::class.java) {
    private lateinit var binding: ActivityLoginBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this

        val reducer = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(this.applicationContext, R.animator.regain_size)

        binding.loginButton.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(view, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(view, increaser)
            false
        }

        binding.loginButton.setOnClickListener {
            model.login(binding.username.editText?.text.toString(), binding.password.editText?.text.toString())
        }

        binding.shareLogLoginButton.setOnClickListener {
            Shared.shareHelper.shareLog(this)
        }

        model.didLogin.observe(this) {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        }

        model.usernameFieldError.observe(this) { error ->
            if (error != null) {
                showError(binding.username, resources.getString(error))
            } else {
                showError(binding.username, null)
            }
        }

        model.passwordFieldError.observe(this) { error ->
            if (error != null) {
                showError(binding.password, resources.getString(error))
            } else {
                showError(binding.password, null)
            }
        }

        model.nodeFieldError.observe(this) { error ->
            if (error != null) {
                showError(binding.nodeListField, resources.getString(error))
            } else {
                showError(binding.nodeListField, null)
            }
        }

        binding.username.editText?.doOnTextChanged { _, _, _, _ ->
            showError(binding.username, null)
        }

        binding.password.editText?.doOnTextChanged { _, _, _, _ ->
            showError(binding.password, null)
        }

        model.username.observe(this) { value ->
            binding.username.editText?.setText(value)
        }

        model.password.observe(this) { value ->
            binding.password.editText?.setText(value)
        }

        val nodes = resources.getStringArray(R.array.node_array)
        val nodeMenu = (binding.nodeListField.editText as? MaterialAutoCompleteTextView)
        nodeMenu?.setSimpleItems(nodes)
        nodeMenu?.setOnItemClickListener { _, _, index, _ ->
            when (index) {
                0 -> model.changeNode(Node.NODE_1)
                1 -> model.changeNode(Node.NODE_2)
                2 -> model.changeNode(Node.NODE_3)
                3 -> model.changeNode(Node.NODE_4)
                4 -> model.changeNode(Node.NODE_5)
                5 -> model.changeNode(Node.NODE_6)
                6 -> model.changeNode(Node.NODE_7)
                7 -> model.changeNode(Node.NODE_8)
                8 -> model.changeNode(Node.NODE_9)
                9 -> model.changeNode(Node.NODE_10)
                10 -> model.changeNode(Node.NODE_11)
            }
        }

        model.node.observe(this) { node ->
            val nodeIndex = when (node) {
                Node.NODE_1 -> 0
                Node.NODE_2 -> 1
                Node.NODE_3 -> 2
                Node.NODE_4 -> 3
                Node.NODE_5 -> 4
                Node.NODE_6 -> 5
                Node.NODE_7 -> 6
                Node.NODE_8 -> 7
                Node.NODE_9 -> 8
                Node.NODE_10 -> 9
                Node.NODE_11 -> 10
                null -> null
            }

            if (nodeIndex != null) {
                nodeMenu?.setText(nodes[nodeIndex], false)
            }
        }
    }

    private fun showError(textView: TextInputLayout, text: String?) {
        textView.error = text
        textView.isErrorEnabled = text != null
        if (text != null) {
            textView.requestFocus()
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}