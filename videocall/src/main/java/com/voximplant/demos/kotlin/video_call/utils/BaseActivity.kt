package com.voximplant.demos.kotlin.video_call.utils

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

abstract class BaseActivity<T : BaseViewModel>(private val modelType: Class<T>) :
    AppCompatActivity() {

    protected val model: T
        get() = ViewModelProvider(ViewModelStoreOwner { this.viewModelStore }).get(modelType)

    private val rootViewGroup: ViewGroup
        get() = window.decorView.rootView as ViewGroup
    private var progressHUDView: ProgressHUDView? = null
    private var errorHUDView: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.showProgress.observe(this, Observer { textID ->
            showProgressHUD(resources.getString(textID))
        })

        model.hideProgress.observe(this, Observer {
            hideProgressHUD()
        })

        model.stringError.observe(this, Observer { text ->
            showError(text)
        })

        model.intError.observe(this, Observer { textID ->
            showError(resources.getString(textID))
        })

        model.finish.observe(this, Observer {
            finish()
        })

        model.onCreate()
    }

    override fun onResume() {
        super.onResume()
        model.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        model.onDestroy()
    }

    private fun showProgressHUD(text: String) {
        progressHUDView?.setText(text)
            ?: run {
                progressHUDView = ProgressHUDView(this)
                progressHUDView?.setText(text)
                rootViewGroup.addView(progressHUDView)
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }
    }

    private fun hideProgressHUD() {
        progressHUDView?.let {
            rootViewGroup.removeView(it)
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressHUDView = null
        }
    }

    private fun showError(text: String) {
        errorHUDView?.setMessage(text)
            ?: run {
                errorHUDView = AlertDialog.Builder(this)
                    .setTitle("Something went wrong")
                    .setMessage(text)
                    .setPositiveButton("Close") { _, _ -> errorHUDView = null }
                    .setCancelable(false)
                    .show()
            }
    }
}