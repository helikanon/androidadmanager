package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.*

class AdmobAppOpenAdManager(
    val application: Application,
    var appOpenPlacementId: String = "ca-app-pub-3940256099942544/3419835294",
    var showListener: AdPlatformShowListener? = null,
    var loadListener: AdPlatformLoadListener? = null


) : AppOpenAdBaseLifeCycle(application), LifecycleObserver {
    var platform = AdPlatformTypeEnum.ADMOB

    private var appOpenAd: AppOpenAd? = null

    // private val adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    private var isShowing = false
    private var loadTime: Long = 0
    var lastShowDate: Date? = null
    var minElapsedSecondsToNextShow = 10
    var isEnable = true
    var excludedActivities = arrayListOf<String>()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /*fun enableTestMode() {
        adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    }*/

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public fun onStart() {
        if (!isEnable) return

        currentActivity?.let {
            var show = true

            if (lastShowDate != null) {
                val x = ((Date().time - lastShowDate!!.time) / 1000)
                show = ((Date().time - lastShowDate!!.time) / 1000) > minElapsedSecondsToNextShow
            }

            if (excludedActivities.contains(it.javaClass.simpleName)) {
                show = false
            }

            if (show) {
                show(it, null)
            }
        } ?: load(null)
    }

    fun load(listener: AdPlatformLoadListener?) {
        if (!isEnable) return

        if (listener != null) {
            loadListener = listener
        }

        if (isAdLoaded()) {
            loadListener?.onLoaded(platform)
            return
        }

        val request: AdRequest = AdRequest.Builder().build();
        AppOpenAd.load(application, appOpenPlacementId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                loadTime = Date().time
                loadListener?.onLoaded(platform)
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                loadListener?.onError(AdErrorMode.PLATFORM, p0?.message, platform)
            }
        })
    }

    fun isAdLoaded(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    fun show(activity: Activity, listener: AdPlatformShowListener? = null) {
        if (!isEnable) return

        if (listener != null) {
            showListener = listener
        }

        if (!isAdLoaded()) {
            showListener?.onError(AdErrorMode.PLATFORM, "${platform.name} adopen >> noads loaded", platform)
            load(null)
            return
        }

        if (isShowing) return

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                super.onAdFailedToShowFullScreenContent(error)
                showListener?.onError(AdErrorMode.PLATFORM, error?.message ?: "", platform)
            }

            override fun onAdShowedFullScreenContent() {
                lastShowDate = Date()
                isShowing = true
                showListener?.onDisplayed(platform)
            }

            override fun onAdDismissedFullScreenContent() {
                lastShowDate = Date() // kapattığında bunu güncellemeliyiz gösterdiğinde güncellememizin nedeni işimizi sağlama alalım
                showListener?.onClosed(platform)

                appOpenAd = null
                isShowing = false
                load(null)
            }

        }
        appOpenAd?.show(activity)
    }

    fun disable() {
        isEnable = false
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - this.loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < (numMilliSecondsPerHour * numHours)
    }
}


open class AppOpenAdBaseLifeCycle(application: Application) : Application.ActivityLifecycleCallbacks {
    protected var currentActivity: Activity? = null

    init {
        // Cannot directly use `this`
        // Issue : Leaking 'this' in constructor of non-final class BaseObserver
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

}