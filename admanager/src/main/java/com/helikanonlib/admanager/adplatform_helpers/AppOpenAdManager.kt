package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date
import java.util.Locale

class AppOpenAdManager(
    val application: Application,
    // var admobAppOpenPlacementId: String = "ca-app-pub-3940256099942544/3419835294",
    var placements: MutableMap<AdPlatformTypeEnum, String> = mutableMapOf(),
    var showOrderStr: String = "admob",
    var globalShowListener: AdPlatformShowListener? = null,
    var globalLoadListener: AdPlatformLoadListener? = null,

    ) {
    //var platform = AdPlatformTypeEnum.ADMOB

    var admobPlacementId = ""
    private var admobAppOpenAd: AppOpenAd? = null
    private var admobLoadTime: Long = 0

    var applovinPlacementId = ""
    private var applovinAppOpenAd: MaxAppOpenAd? = null
    private var applovinLoadTime: Long = 0


    // private val adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    @Volatile
    private var isShowing = false
    private var isAdmobLoading = false
    private var isApplovinLoading = false
    private val pendingAdmobLoadListeners = mutableListOf<AdPlatformLoadListener>()
    private val pendingApplovinLoadListeners = mutableListOf<AdPlatformLoadListener>()

    var lastShowDate: Date? = null
    var minElapsedSecondsToNextShow = 10
    /** When false, neither loading nor showing is allowed. */
    var isEnabled = true

    /**
     * Backwards-compatible alias. Prefer [isEnabled], [enable] and [disable].
     */
    @Deprecated("Use isEnabled instead", ReplaceWith("isEnabled"))
    var isEnable: Boolean
        get() = isEnabled
        set(value) {
            isEnabled = value
        }

    /** Temporarily controls showing without preventing ads from loading. */
    var isShowingEnabled = true
        private set

    var excludedActivities = arrayListOf<String>()


    init {
        admobPlacementId = placements[AdPlatformTypeEnum.ADMOB].orEmpty()
        applovinPlacementId = placements[AdPlatformTypeEnum.APPLOVIN].orEmpty()
        showOrderStr = normalizeShowOrder(showOrderStr).joinToString(",")

        if ("applovin" in normalizeShowOrder(showOrderStr) && applovinPlacementId.isNotEmpty()) {
            applovinAppOpenAd = MaxAppOpenAd(applovinPlacementId)
        }

    }

    /*fun enableTestMode() {
        adOpenPlacementId = "ca-app-pub-3940256099942544/3419835294"
    }*/

    /** Shows immediately using the configured platform order. */
    @JvmOverloads
    fun show(activity: Activity, listener: AdPlatformShowListener? = null) {
        show(showOrderStr, activity, listener)
    }

    /** Shows only when the minimum interval has elapsed and the activity is not excluded. */
    @JvmOverloads
    fun showIntervalElapsed(activity: Activity, listener: AdPlatformShowListener? = null) {
        if (!isEnabled || !isShowingEnabled || isActivityExcluded(activity) || !hasShowIntervalElapsed()) return
        show(activity, listener)
    }

    fun isActivityExcluded(activity: Activity): Boolean {
        return activity.javaClass.simpleName in excludedActivities || activity.javaClass.name in excludedActivities
    }

    fun hasShowIntervalElapsed(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return AppOpenAdPolicy.hasShowIntervalElapsed(lastShowDate?.time, minElapsedSecondsToNextShow, nowMillis)
    }

    @JvmOverloads
    fun load(listener: AdPlatformLoadListener? = null) {
        if (!isEnabled) return

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

        val showOrder = normalizeShowOrder(showOrderStr)
        var didStartLoad = false
        if ("admob" in showOrder && admobPlacementId.isNotEmpty()) {
            didStartLoad = true
            loadAdmob(_listener)
        }

        if ("applovin" in showOrder && applovinAppOpenAd != null && applovinPlacementId.isNotEmpty()) {
            didStartLoad = true
            loadApplovin(_listener)
        }

        if (!didStartLoad) {
            _listener.onError(AdErrorMode.MANAGER, "No app open placement is configured for the show order", null)
        }
    }

    fun show(showOrder: String, activity: Activity, listener: AdPlatformShowListener? = null) {
        if (!isEnabled || !isShowingEnabled) return
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

                load()
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

                load()
            }

        }

        var isShowed = false
        val showOrderArr = normalizeShowOrder(showOrder)

        for (i in 0 until showOrderArr.size) {
            val platformName = showOrderArr[i]
            if (platformName == "admob" && isAdmobAdLoaded()) {
                isShowed = true
                isShowing = true
                showAdmob(activity, _listener)
                break
            } else if (platformName == "applovin" && isApplovinAdLoaded()) {
                isShowed = true
                isShowing = true
                showApplovin(activity, _listener)
                break
            }
        }

        if (!isShowed) {
            load()
            globalShowListener?.onError(AdErrorMode.MANAGER, "adopen >> noads loaded to show", null)
            listener?.onError(AdErrorMode.MANAGER, "adopen >> noads loaded to show", null)
        }
    }

    fun disable() {
        isEnabled = false
    }

    fun enable() {
        isEnabled = true
    }

    /** Prevents show calls while still allowing preloading. */
    fun pauseShowing() {
        isShowingEnabled = false
    }

    /** Re-enables show calls. This does not show an ad automatically. */
    fun resumeShowing() {
        isShowingEnabled = true
    }


    // APPLOVIN
    fun loadApplovin(listener: AdPlatformLoadListener?) {
        val platform = AdPlatformTypeEnum.APPLOVIN

        if (!isEnabled) return
        if (applovinPlacementId.isBlank() || applovinAppOpenAd == null) {
            listener?.onError(AdErrorMode.MANAGER, "APPLOVIN app open placement is not configured", platform)
            return
        }

        if (isApplovinAdLoaded()) {
            listener?.onLoaded(platform)
            return
        }
        listener?.let(pendingApplovinLoadListeners::add)
        if (isApplovinLoading) return
        isApplovinLoading = true
        applovinAppOpenAd?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                isApplovinLoading = false
                if (!isEnabled) {
                    applovinLoadTime = 0
                    pendingApplovinLoadListeners.clear()
                    return
                }
                applovinLoadTime = Date().time
                pendingApplovinLoadListeners.toList().also(pendingApplovinLoadListeners::removeAll)
                    .forEach { it.onLoaded(platform) }
            }

            override fun onAdDisplayed(ad: MaxAd) {

            }

            override fun onAdHidden(ad: MaxAd) {

            }

            override fun onAdClicked(ad: MaxAd) {
                // Load listeners do not expose click events.
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                isApplovinLoading = false
                if (!isEnabled) {
                    pendingApplovinLoadListeners.clear()
                    return
                }
                pendingApplovinLoadListeners.toList().also(pendingApplovinLoadListeners::removeAll)
                    .forEach { it.onError(AdErrorMode.PLATFORM, error.message, platform) }
            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {

            }

        })
        applovinAppOpenAd?.loadAd()
    }

    fun showApplovin(@Suppress("UNUSED_PARAMETER") activity: Activity, listener: AdPlatformShowListener? = null) {
        val platform = AdPlatformTypeEnum.APPLOVIN

        if (!isEnabled || !isShowingEnabled) return

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
                listener?.onClicked(platform)
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {

            }

            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                listener?.onError(AdErrorMode.PLATFORM, error.message, platform)
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

        if (!isEnabled) return
        if (admobPlacementId.isBlank()) {
            listener?.onError(AdErrorMode.MANAGER, "ADMOB app open placement is not configured", platform)
            return
        }

        if (isAdmobAdLoaded()) {
            listener?.onLoaded(platform)
            return
        }
        listener?.let(pendingAdmobLoadListeners::add)
        if (isAdmobLoading) return
        isAdmobLoading = true

        val request: AdRequest = AdRequest.Builder().build()
        // AppOpenAd.load(application, admobPlacementId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, object : AppOpenAd.AppOpenAdLoadCallback() {
        AppOpenAd.load(application, admobPlacementId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                isAdmobLoading = false
                if (!isEnabled) {
                    admobAppOpenAd = null
                    admobLoadTime = 0
                    pendingAdmobLoadListeners.clear()
                    return
                }
                admobAppOpenAd = ad
                admobLoadTime = Date().time

                pendingAdmobLoadListeners.toList().also(pendingAdmobLoadListeners::removeAll)
                    .forEach { it.onLoaded(platform) }
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                isAdmobLoading = false
                admobAppOpenAd = null
                admobLoadTime = 0
                if (!isEnabled) {
                    pendingAdmobLoadListeners.clear()
                    return
                }
                pendingAdmobLoadListeners.toList().also(pendingAdmobLoadListeners::removeAll)
                    .forEach { it.onError(AdErrorMode.PLATFORM, p0.message, platform) }
            }
        })
    }


    fun showAdmob(activity: Activity, listener: AdPlatformShowListener? = null) {
        val platform = AdPlatformTypeEnum.ADMOB

        if (!isEnabled || !isShowingEnabled) return

        if (!isAdmobAdLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} adopen >> noads loaded", platform)
            return
        }

        admobAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                super.onAdFailedToShowFullScreenContent(error)
                admobAppOpenAd = null
                admobLoadTime = 0
                listener?.onError(AdErrorMode.PLATFORM, error.message, platform)
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

    private fun normalizeShowOrder(showOrder: String): List<String> {
        return showOrder.split(',')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

}

internal object AppOpenAdPolicy {
    fun hasShowIntervalElapsed(lastShowMillis: Long?, minimumSeconds: Int, nowMillis: Long): Boolean {
        if (lastShowMillis == null) return true
        val minimumIntervalMillis = minimumSeconds.coerceAtLeast(0) * 1_000L
        return nowMillis - lastShowMillis >= minimumIntervalMillis
    }
}
