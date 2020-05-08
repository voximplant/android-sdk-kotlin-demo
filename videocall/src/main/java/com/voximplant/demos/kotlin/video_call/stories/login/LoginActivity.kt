package com.voximplant.demos.kotlin.video_call.stories.login

import android.animation.Animator
import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.voximplant.demos.kotlin.video_call.R
import com.voximplant.demos.kotlin.video_call.stories.main.MainActivity
import com.voximplant.demos.kotlin.video_call.utils.BaseActivity
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.MainScope

class LoginActivity: BaseActivity<LoginViewModel>(LoginViewModel::class.java) {

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

        model.didLogin.observe(this, Observer {
            Intent(this, MainActivity::class.java).also {
                startActivity(it)
            }
        })

        model.invalidInputError.observe(this, Observer {
            showError(
                when (it.first) {
                    true -> usernameView
                    false -> passwordView
                }
                , resources.getString(it.second)
            )
        })

        model.usernameFieldText.observe(this, Observer {
            usernameView.setText(it)
        })

        model.passwordFieldText.observe(this, Observer {
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