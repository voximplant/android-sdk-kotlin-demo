package com.voximplant.demos.kotlin.video_call.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voximplant.demos.kotlin.video_call.services.AuthServiceListener
import kotlinx.coroutines.launch

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