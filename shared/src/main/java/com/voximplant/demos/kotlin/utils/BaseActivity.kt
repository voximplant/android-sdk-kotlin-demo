package com.voximplant.demos.kotlin.utils

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

abstract class BaseActivity<T : BaseViewModel>(private val modelType: Class<T>) :
    AppCompatActivity() {

    protected val model: T
        get() = ViewModelProvider (this)[modelType]

    private val rootViewGroup: ViewGroup
        get() = window.decorView.rootView as ViewGroup
    private var progressHUDView: ProgressHUDView? = null
    private var errorHUDView: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.showProgress.observe(this) { textID ->
            showProgressHUD(resources.getString(textID))
        }

        model.hideProgress.observe(this) {
            hideProgressHUD()
        }

        model.stringError.observe(this) { text ->
            showError(text)
        }

        model.intError.observe(this) { textID ->
            showError(resources.getString(textID))
        }

        model.finish.observe(this) {
            finish()
        }

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
                    .setTitle(getString(R.string.something_went_wrong))
                    .setMessage(text)
                    .setNegativeButton(getString(R.string.close)) { _, _ -> errorHUDView = null }
                    .setCancelable(false)
                    .show()
            }
    }
}