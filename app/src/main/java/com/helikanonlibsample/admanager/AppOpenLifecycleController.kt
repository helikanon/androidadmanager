package com.helikanonlibsample.admanager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.helikanonlib.admanager.AppOpenAdManager
import java.lang.ref.WeakReference

class AppOpenLifecycleController(
    private val application: Application,
    private val managerProvider: () -> AppOpenAdManager?
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var currentActivity = WeakReference<Activity>(null)
    private var shouldShowOnNextStart = false
    private var isRegistered = false

    fun register() {
        if (isRegistered) return
        isRegistered = true

        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unregister() {
        if (!isRegistered) return
        isRegistered = false

        application.unregisterActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        currentActivity.clear()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!shouldShowOnNextStart) return
        shouldShowOnNextStart = false

        currentActivity.get()
            ?.takeUnless { it.isFinishing || it.isDestroyed }
            ?.let { managerProvider()?.showIntervalElapsed(it) }
    }

    override fun onStop(owner: LifecycleOwner) {
        shouldShowOnNextStart = true
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity.get() === activity) currentActivity.clear()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
