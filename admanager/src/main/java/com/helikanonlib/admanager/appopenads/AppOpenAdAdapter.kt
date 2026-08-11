package com.helikanonlib.admanager

import android.app.Activity

internal interface AppOpenAdAdapter {
    val platform: AdPlatformTypeEnum

    fun isReady(validityDurationMillis: Long, nowElapsedRealtime: Long): Boolean
    fun load(callback: AppOpenAdLoadCallback)
    fun show(activity: Activity, callback: AppOpenAdDisplayCallback): Boolean
    fun invalidate()
}

internal interface AppOpenAdLoadCallback {
    fun onLoaded()
    fun onError(message: String)
}

internal interface AppOpenAdDisplayCallback {
    fun onDisplayed()
    fun onClicked()
    fun onClosed()
    fun onError(message: String)
}
