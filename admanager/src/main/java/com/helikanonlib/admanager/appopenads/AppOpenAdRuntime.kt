package com.helikanonlib.admanager

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

internal interface AppOpenAdRuntime {
    fun isMainThread(): Boolean
    fun post(action: () -> Unit)
    fun elapsedRealtime(): Long
}

internal class AndroidAppOpenAdRuntime : AppOpenAdRuntime {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    override fun post(action: () -> Unit) {
        mainHandler.post(action)
    }

    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
