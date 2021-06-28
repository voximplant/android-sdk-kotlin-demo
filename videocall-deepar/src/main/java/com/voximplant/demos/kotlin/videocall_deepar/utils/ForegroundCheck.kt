package com.voximplant.demos.kotlin.videocall_deepar.utils

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

class ForegroundCheck : ActivityLifecycleCallbacks {
    var isInForeground = false
        private set

    override fun onActivityResumed(activity: Activity) {
        isInForeground = true
    }

    override fun onActivityPaused(activity: Activity) {
        isInForeground = false
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) { }

    override fun onActivityDestroyed(activity: Activity) {}
}