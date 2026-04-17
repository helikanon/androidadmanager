package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.*

class AppOpenAdManager(
    val application: Application,
    // var admobAppOpenPlacementId: String = "ca-app-pub-3940256099942544/3419835294",
    var placements: MutableMap<AdPlatformTypeEnum, String> = mutableMapOf(),
    var showOrderStr: String = "admob",
    var globalShowListener: AdPlatformShowListener? = null,
    var globalLoadListener: AdPlatformLoadListener? = null,

    ) : AppOpenAdBaseLifeCycle(application), LifecycleObserver {
    //var platform = AdPlatformTypeEnum.ADMOB

    var admobPlacementId = ""
    private var admobAppOpenAd: AppOpenAd? = null
    private var admobLoadTime: Long = 0

    var applovinPlacementId = ""
    private var applovinAppOpenAd: MaxAppOpenAd? = null
    private var applovinLoadTime: Long = 0


    // private val adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    private var isShowing = false

    var lastShowDate: Date? = null
    var minElapsedSecondsToNextShow = 10
    var isEnable = true
    var excludedActivities = arrayListOf<String>()


    init {
        admobPlacementId = placements.get(AdPlatformTypeEnum.ADMOB) ?: ""
        applovinPlacementId = placements.get(AdPlatformTypeEnum.APPLOVIN) ?: ""
        showOrderStr = showOrderStr.lowercase()

        if (showOrderStr.contains("applovin") && applovinPlacementId.isNotEmpty()) {
            applovinAppOpenAd = MaxAppOpenAd(applovinPlacementId, application.applicationContext)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // load(null)
    }

    /*fun enableTestMode() {
        adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    }*/

    // @OnLifecycleEvent(Lifecycle.Event.ON_START)
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
                show(showOrderStr, it, null)

            }
        } ?: load(null)
    }


    fun load(listener: AdPlatformLoadListener?) {

        val _listener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)

                globalLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)

                globalLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

        }

        if (showOrderStr.contains("admob") && admobPlacementId.isNotEmpty()) {
            loadAdmob(_listener)
        }

        if (showOrderStr.contains("applovin") && applovinAppOpenAd != null && applovinPlacementId.isNotEmpty()) {
            loadApplovin(_listener)
        }
    }

    fun show(showOrder: String, activity: Activity, listener: AdPlatformShowListener? = null) {
        if (!isEnable) return
        if (isShowing) return

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onDisplayed(adPlatformEnum)
                isShowing = true
                lastShowDate = Date()

                globalShowListener?.onDisplayed(adPlatformEnum)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onClicked(adPlatformEnum)

                globalShowListener?.onClicked(adPlatformEnum)
                listener?.onClicked(adPlatformEnum)
            }

            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onClosed(adPlatformEnum)

                isShowing = false
                lastShowDate = Date() // kapattığında bunu güncellemeliyiz gösterdiğinde güncellememizin nedeni işimizi sağlama alalım
                globalShowListener?.onClosed(adPlatformEnum)
                listener?.onClosed(adPlatformEnum)

                load(null)
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onRewarded(type, amount, adPlatformEnum)

                globalShowListener?.onRewarded(type, amount, adPlatformEnum)
                listener?.onRewarded(type, amount, adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)

                isShowing = false

                globalShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

        }

        var isShowed = false
        val showOrderArr = showOrder.split(",")

        for (i in 0 until showOrderArr.size) {
            val platformName = showOrderArr[i]
            if (platformName.lowercase() == "admob" && isAdmobAdLoaded()) {
                isShowed = true
                showAdmob(activity, _listener)
                break
            } else if (platformName.lowercase() == "applovin" && isApplovinAdLoaded()) {
                isShowed = true
                showApplovin(activity, _listener)
                break
            }
        }

        if (!isShowed) {
            load(null)
            globalShowListener?.onError(AdErrorMode.MANAGER, "adopen >> noads loaded to show", null)
            listener?.onError(AdErrorMode.MANAGER, "adopen >> noads loaded to show", null)
        }
    }

    fun disable() {
        isEnable = false
    }


    // APPLOVIN
    fun loadApplovin(listener: AdPlatformLoadListener?) {
        val platform = AdPlatformTypeEnum.APPLOVIN

        applovinAppOpenAd?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                applovinLoadTime = Date().time
                listener?.onLoaded(platform)
            }

            override fun onAdDisplayed(ad: MaxAd) {

            }

            override fun onAdHidden(ad: MaxAd) {

            }

            override fun onAdClicked(ad: MaxAd) {

            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                listener?.onError(AdErrorMode.PLATFORM, error?.message, platform)
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {

            }

        })
        applovinAppOpenAd?.loadAd()
    }

    fun showApplovin(activity: Activity, listener: AdPlatformShowListener? = null) {
        val platform = AdPlatformTypeEnum.APPLOVIN

        if (!isEnable) return

        if (!isApplovinAdLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} adopen >> noads loaded", platform)
            return
        }

        applovinAppOpenAd?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
            }

            override fun onAdDisplayed(ad: MaxAd) {
                listener?.onDisplayed(platform)
            }

            override fun onAdHidden(ad: MaxAd) {
                listener?.onClosed(platform)
            }

            override fun onAdClicked(ad: MaxAd) {

            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {

            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                listener?.onError(AdErrorMode.PLATFORM, error?.message ?: "", platform)
            }

        })
        applovinAppOpenAd?.showAd()
    }


    private fun applovinWasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - this.applovinLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < (numMilliSecondsPerHour * numHours)
    }

    fun isApplovinAdLoaded(): Boolean {
        return applovinAppOpenAd != null
                && applovinWasLoadTimeLessThanNHoursAgo(4)
                && applovinAppOpenAd?.isReady ?: false
    }

    // ADMOB
    fun loadAdmob(listener: AdPlatformLoadListener?) {
        val platform = AdPlatformTypeEnum.ADMOB

        if (isAdmobAdLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        val request: AdRequest = AdRequest.Builder().build();
        // AppOpenAd.load(application, admobPlacementId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, object : AppOpenAd.AppOpenAdLoadCallback() {
        AppOpenAd.load(application, admobPlacementId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                admobAppOpenAd = ad
                admobLoadTime = Date().time

                listener?.onLoaded(platform)
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                listener?.onError(AdErrorMode.PLATFORM, p0?.message, platform)
            }
        })
    }


    fun showAdmob(activity: Activity, listener: AdPlatformShowListener? = null) {
        val platform = AdPlatformTypeEnum.ADMOB

        if (!isEnable) return

        if (!isAdmobAdLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} adopen >> noads loaded", platform)
            return
        }

        admobAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                super.onAdFailedToShowFullScreenContent(error)
                listener?.onError(AdErrorMode.PLATFORM, error?.message ?: "", platform)
            }

            override fun onAdShowedFullScreenContent() {
                //lastShowDate = Date()
                listener?.onDisplayed(platform)
            }

            override fun onAdDismissedFullScreenContent() {
                // lastShowDate = Date() // kapattığında bunu güncellemeliyiz gösterdiğinde güncellememizin nedeni işimizi sağlama alalım
                admobAppOpenAd = null

                listener?.onClosed(platform)
            }

        }
        admobAppOpenAd?.show(activity)
    }


    private fun admobWasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - this.admobLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < (numMilliSecondsPerHour * numHours)
    }

    fun isAdmobAdLoaded(): Boolean {
        return admobAppOpenAd != null && admobWasLoadTimeLessThanNHoursAgo(4)
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