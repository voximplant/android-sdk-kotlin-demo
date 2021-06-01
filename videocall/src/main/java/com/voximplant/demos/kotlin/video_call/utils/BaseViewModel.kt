package com.voximplant.demos.kotlin.video_call.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {
    val showProgress = MutableLiveData<Int>()
    val hideProgress = MutableLiveData<Unit>()
    val finish = MutableLiveData<Unit>()
    val intError = MutableLiveData<Int>()
    val stringError = MutableLiveData<String>()

    protected fun <T : Comparable<T>> postError(error: T) {
        when (error) {
            is Int -> intError.postValue(error)
            is String -> stringError.postValue(error)
            is AuthError -> stringError.postValue(error.description)
        }
    }

    open fun onCreate() { }
    open fun onResume() { }
    open fun onDestroy() { }
}