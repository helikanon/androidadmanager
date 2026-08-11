package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
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

enum class AppOpenAdShowResult {
    SHOW_REQUESTED,
    QUEUED_ON_MAIN_THREAD,
    DISABLED,
    SHOWING_PAUSED,
    ALREADY_SHOWING,
    INVALID_ACTIVITY,
    ACTIVITY_EXCLUDED,
    INTERVAL_NOT_ELAPSED,
    AD_NOT_READY,
    NO_CONFIGURED_PLATFORM,
    REQUEST_FAILED
}

class AppOpenAdManager(
    private val application: Application,
    placements: Map<AdPlatformTypeEnum, String> = emptyMap(),
    showOrderStr: String = "admob",
    var globalShowListener: AdPlatformShowListener? = null,
    var globalLoadListener: AdPlatformLoadListener? = null,
    adValidityDurationMillis: Long = DEFAULT_AD_VALIDITY_DURATION_MILLIS
) {
    val placements: Map<AdPlatformTypeEnum, String> = placements.toMap()
    val showOrderStr: String = normalizeShowOrder(showOrderStr).joinToString(",")
    val adValidityDurationMillis: Long = adValidityDurationMillis.also {
        require(it > 0L) { "adValidityDurationMillis must be greater than zero" }
    }

    private val defaultShowOrder = normalizeShowOrder(this.showOrderStr)
    private val admobPlacementId = this.placements[AdPlatformTypeEnum.ADMOB].orEmpty()
    private val applovinPlacementId = this.placements[AdPlatformTypeEnum.APPLOVIN].orEmpty()
    private val applovinAppOpenAd: MaxAppOpenAd? = applovinPlacementId
        .takeIf(String::isNotBlank)
        ?.let(::MaxAppOpenAd)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var admobAppOpenAd: AppOpenAd? = null
    private var admobLoadElapsedRealtime = 0L
    private var applovinLoadElapsedRealtime = 0L

    private var isShowing = false
    private var isAdmobLoading = false
    private var isApplovinLoading = false
    private var admobLoadGeneration = 0
    private var applovinLoadGeneration = 0
    private val pendingAdmobLoadListeners = mutableListOf<AdPlatformLoadListener>()
    private val pendingApplovinLoadListeners = mutableListOf<AdPlatformLoadListener>()

    private var lastShowElapsedRealtime: Long? = null
    var lastShowDate: Date? = null
        private set

    var minElapsedSecondsToNextShow = 10

    var isEnabled = true
        private set

    var isShowingEnabled = true
        private set

    var excludedActivities = arrayListOf<String>()

    /** Shows immediately using the configured platform order. */
    @CheckResult
    @JvmOverloads
    fun show(activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(defaultShowOrder, activity, listener, checkIntervalAndExclusion = false)
    }

    /** Shows immediately using a one-off platform order. */
    @CheckResult
    @JvmOverloads
    fun show(showOrder: String, activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(normalizeShowOrder(showOrder), activity, listener, checkIntervalAndExclusion = false)
    }

    /** Shows only when the minimum interval has elapsed and the activity is not excluded. */
    @CheckResult
    @JvmOverloads
    fun showIntervalElapsed(activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(defaultShowOrder, activity, listener, checkIntervalAndExclusion = true)
    }

    private fun show(
        showOrder: List<String>,
        activity: Activity,
        listener: AdPlatformShowListener?,
        checkIntervalAndExclusion: Boolean
    ): AppOpenAdShowResult {
        if (!isMainThread()) {
            mainHandler.post { show(showOrder, activity, listener, checkIntervalAndExclusion) }
            return AppOpenAdShowResult.QUEUED_ON_MAIN_THREAD
        }

        if (!isEnabled) return AppOpenAdShowResult.DISABLED
        if (!isShowingEnabled) return AppOpenAdShowResult.SHOWING_PAUSED
        if (activity.isFinishing || activity.isDestroyed) return AppOpenAdShowResult.INVALID_ACTIVITY
        if (isShowing) return AppOpenAdShowResult.ALREADY_SHOWING

        if (checkIntervalAndExclusion) {
            if (isActivityExcluded(activity)) return AppOpenAdShowResult.ACTIVITY_EXCLUDED
            if (!hasShowIntervalElapsed()) return AppOpenAdShowResult.INTERVAL_NOT_ELAPSED
        }

        val configuredOrder = showOrder.filter(::isPlatformConfigured)
        if (configuredOrder.isEmpty()) {
            notifyShowError(listener, "No app open placement is configured for the show order", null)
            return AppOpenAdShowResult.NO_CONFIGURED_PLATFORM
        }

        val selectedPlatform = configuredOrder.firstOrNull { platformName ->
            when (platformName) {
                ADMOB -> isAdmobAdLoaded()
                APPLOVIN -> isApplovinAdLoaded()
                else -> false
            }
        }

        if (selectedPlatform == null) {
            load(configuredOrder, null)
            notifyShowError(listener, "App open ad is not ready; a load was requested", null)
            return AppOpenAdShowResult.AD_NOT_READY
        }

        isShowing = true
        val forwardingListener = createShowListener(listener, configuredOrder)
        val requested = when (selectedPlatform) {
            ADMOB -> requestAdmobShow(activity, forwardingListener)
            APPLOVIN -> requestApplovinShow(forwardingListener)
            else -> false
        }

        if (requested != true) {
            isShowing = false
            return AppOpenAdShowResult.REQUEST_FAILED
        }
        return AppOpenAdShowResult.SHOW_REQUESTED
    }

    fun isActivityExcluded(activity: Activity): Boolean {
        return activity.javaClass.simpleName in excludedActivities || activity.javaClass.name in excludedActivities
    }

    fun hasShowIntervalElapsed(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Boolean {
        return AppOpenAdPolicy.hasShowIntervalElapsed(
            lastShowElapsedRealtime,
            minElapsedSecondsToNextShow,
            nowElapsedRealtime
        )
    }

    @JvmOverloads
    fun load(listener: AdPlatformLoadListener? = null) {
        runOnMain { load(defaultShowOrder, listener) }
    }

    @MainThread
    private fun load(showOrder: List<String>, listener: AdPlatformLoadListener?) {
        if (!isEnabled) {
            notifyLoadError(listener, DISABLED_LOAD_MESSAGE, null)
            return
        }

        val configuredOrder = showOrder.filter(::isPlatformConfigured)
        if (configuredOrder.isEmpty()) {
            notifyLoadError(listener, "No app open placement is configured for the show order", null)
            return
        }

        val forwardingListener = createLoadListener(listener)
        if (ADMOB in configuredOrder) enqueueAdmobLoad(forwardingListener)
        if (APPLOVIN in configuredOrder) enqueueApplovinLoad(forwardingListener)
    }

    fun disable() {
        runOnMain {
            if (!isEnabled) return@runOnMain
            isEnabled = false
            admobLoadGeneration++
            applovinLoadGeneration++
            cancelPendingLoads()
        }
    }

    fun enable() {
        runOnMain { isEnabled = true }
    }

    /** Prevents show calls while still allowing preloading. */
    fun pauseShowing() {
        runOnMain { isShowingEnabled = false }
    }

    /** Re-enables show calls. This does not show an ad automatically. */
    fun resumeShowing() {
        runOnMain { isShowingEnabled = true }
    }

    @MainThread
    private fun enqueueAdmobLoad(listener: AdPlatformLoadListener) {
        if (isAdmobAdLoaded()) {
            safeListenerCall { listener.onLoaded(AdPlatformTypeEnum.ADMOB) }
            return
        }

        pendingAdmobLoadListeners.add(listener)
        if (!isAdmobLoading) startAdmobLoad()
    }

    @MainThread
    private fun startAdmobLoad() {
        if (!isEnabled || isAdmobLoading || pendingAdmobLoadListeners.isEmpty()) return
        isAdmobLoading = true
        val requestGeneration = admobLoadGeneration

        try {
            AppOpenAd.load(
                application,
                admobPlacementId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        runOnMain { handleAdmobLoaded(requestGeneration, ad) }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        runOnMain { handleAdmobLoadFailed(requestGeneration, error.message) }
                    }
                }
            )
        } catch (error: Exception) {
            isAdmobLoading = false
            drainAdmobLoadListeners().forEach {
                safeListenerCall { it.onError(AdErrorMode.PLATFORM, error.message.orEmpty(), AdPlatformTypeEnum.ADMOB) }
            }
        }
    }

    @MainThread
    private fun handleAdmobLoaded(requestGeneration: Int, ad: AppOpenAd) {
        isAdmobLoading = false
        if (requestGeneration != admobLoadGeneration || !isEnabled) {
            admobAppOpenAd = null
            admobLoadElapsedRealtime = 0L
            if (isEnabled && pendingAdmobLoadListeners.isNotEmpty()) startAdmobLoad()
            return
        }

        admobAppOpenAd = ad
        admobLoadElapsedRealtime = SystemClock.elapsedRealtime()
        drainAdmobLoadListeners().forEach {
            safeListenerCall { it.onLoaded(AdPlatformTypeEnum.ADMOB) }
        }
    }

    @MainThread
    private fun handleAdmobLoadFailed(requestGeneration: Int, errorMessage: String) {
        isAdmobLoading = false
        admobAppOpenAd = null
        admobLoadElapsedRealtime = 0L
        if (requestGeneration != admobLoadGeneration || !isEnabled) {
            if (isEnabled && pendingAdmobLoadListeners.isNotEmpty()) startAdmobLoad()
            return
        }

        drainAdmobLoadListeners().forEach {
            safeListenerCall { it.onError(AdErrorMode.PLATFORM, errorMessage, AdPlatformTypeEnum.ADMOB) }
        }
    }

    @MainThread
    private fun enqueueApplovinLoad(listener: AdPlatformLoadListener) {
        if (isApplovinAdLoaded()) {
            safeListenerCall { listener.onLoaded(AdPlatformTypeEnum.APPLOVIN) }
            return
        }

        pendingApplovinLoadListeners.add(listener)
        if (!isApplovinLoading) startApplovinLoad()
    }

    @MainThread
    private fun startApplovinLoad() {
        val appOpenAd = applovinAppOpenAd ?: return
        if (!isEnabled || isApplovinLoading || pendingApplovinLoadListeners.isEmpty()) return
        isApplovinLoading = true
        val requestGeneration = applovinLoadGeneration

        appOpenAd.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                runOnMain { handleApplovinLoaded(requestGeneration) }
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                runOnMain { handleApplovinLoadFailed(requestGeneration, error.message) }
            }

            override fun onAdDisplayed(ad: MaxAd) = Unit
            override fun onAdHidden(ad: MaxAd) = Unit
            override fun onAdClicked(ad: MaxAd) = Unit
            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) = Unit
        })

        try {
            appOpenAd.loadAd()
        } catch (error: Exception) {
            isApplovinLoading = false
            drainApplovinLoadListeners().forEach {
                safeListenerCall { it.onError(AdErrorMode.PLATFORM, error.message.orEmpty(), AdPlatformTypeEnum.APPLOVIN) }
            }
        }
    }

    @MainThread
    private fun handleApplovinLoaded(requestGeneration: Int) {
        isApplovinLoading = false
        if (requestGeneration != applovinLoadGeneration || !isEnabled) {
            applovinLoadElapsedRealtime = 0L
            if (isEnabled && pendingApplovinLoadListeners.isNotEmpty()) startApplovinLoad()
            return
        }

        applovinLoadElapsedRealtime = SystemClock.elapsedRealtime()
        drainApplovinLoadListeners().forEach {
            safeListenerCall { it.onLoaded(AdPlatformTypeEnum.APPLOVIN) }
        }
    }

    @MainThread
    private fun handleApplovinLoadFailed(requestGeneration: Int, errorMessage: String) {
        isApplovinLoading = false
        applovinLoadElapsedRealtime = 0L
        if (requestGeneration != applovinLoadGeneration || !isEnabled) {
            if (isEnabled && pendingApplovinLoadListeners.isNotEmpty()) startApplovinLoad()
            return
        }

        drainApplovinLoadListeners().forEach {
            safeListenerCall { it.onError(AdErrorMode.PLATFORM, errorMessage, AdPlatformTypeEnum.APPLOVIN) }
        }
    }

    @MainThread
    private fun requestAdmobShow(activity: Activity, listener: AdPlatformShowListener): Boolean? {
        val ad = admobAppOpenAd ?: return null
        return try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    runOnMain {
                        admobAppOpenAd = null
                        admobLoadElapsedRealtime = 0L
                        listener.onError(AdErrorMode.PLATFORM, error.message, AdPlatformTypeEnum.ADMOB)
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    runOnMain { listener.onDisplayed(AdPlatformTypeEnum.ADMOB) }
                }

                override fun onAdDismissedFullScreenContent() {
                    runOnMain {
                        admobAppOpenAd = null
                        admobLoadElapsedRealtime = 0L
                        listener.onClosed(AdPlatformTypeEnum.ADMOB)
                    }
                }
            }
            ad.show(activity)
            true
        } catch (error: Exception) {
            admobAppOpenAd = null
            admobLoadElapsedRealtime = 0L
            listener.onError(AdErrorMode.PLATFORM, error.message.orEmpty(), AdPlatformTypeEnum.ADMOB)
            false
        }
    }

    @MainThread
    private fun requestApplovinShow(listener: AdPlatformShowListener): Boolean? {
        val appOpenAd = applovinAppOpenAd ?: return null
        return try {
            appOpenAd.setListener(object : MaxAdListener {
                override fun onAdDisplayed(ad: MaxAd) {
                    runOnMain { listener.onDisplayed(AdPlatformTypeEnum.APPLOVIN) }
                }

                override fun onAdHidden(ad: MaxAd) {
                    runOnMain {
                        applovinLoadElapsedRealtime = 0L
                        listener.onClosed(AdPlatformTypeEnum.APPLOVIN)
                    }
                }

                override fun onAdClicked(ad: MaxAd) {
                    runOnMain { listener.onClicked(AdPlatformTypeEnum.APPLOVIN) }
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    runOnMain {
                        applovinLoadElapsedRealtime = 0L
                        listener.onError(AdErrorMode.PLATFORM, error.message, AdPlatformTypeEnum.APPLOVIN)
                    }
                }

                override fun onAdLoaded(ad: MaxAd) = Unit
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) = Unit
            })
            appOpenAd.showAd()
            true
        } catch (error: Exception) {
            applovinLoadElapsedRealtime = 0L
            listener.onError(AdErrorMode.PLATFORM, error.message.orEmpty(), AdPlatformTypeEnum.APPLOVIN)
            false
        }
    }

    private fun createShowListener(
        listener: AdPlatformShowListener?,
        showOrder: List<String>
    ) = object : AdPlatformShowListener() {
        override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
            recordShowTime()
            safeListenerCall { globalShowListener?.onDisplayed(adPlatformEnum) }
            safeListenerCall { listener?.onDisplayed(adPlatformEnum) }
        }

        override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
            safeListenerCall { globalShowListener?.onClicked(adPlatformEnum) }
            safeListenerCall { listener?.onClicked(adPlatformEnum) }
        }

        override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
            isShowing = false
            recordShowTime()
            safeListenerCall { globalShowListener?.onClosed(adPlatformEnum) }
            safeListenerCall { listener?.onClosed(adPlatformEnum) }
            load(showOrder, null)
        }

        override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
            safeListenerCall { globalShowListener?.onRewarded(type, amount, adPlatformEnum) }
            safeListenerCall { listener?.onRewarded(type, amount, adPlatformEnum) }
        }

        override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
            isShowing = false
            safeListenerCall { globalShowListener?.onError(errorMode, errorMessage, adPlatformEnum) }
            safeListenerCall { listener?.onError(errorMode, errorMessage, adPlatformEnum) }
            load(showOrder, null)
        }
    }

    private fun createLoadListener(listener: AdPlatformLoadListener?) = object : AdPlatformLoadListener() {
        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
            safeListenerCall { globalLoadListener?.onLoaded(adPlatformEnum) }
            safeListenerCall { listener?.onLoaded(adPlatformEnum) }
        }

        override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
            safeListenerCall { globalLoadListener?.onError(errorMode, errorMessage, adPlatformEnum) }
            safeListenerCall { listener?.onError(errorMode, errorMessage, adPlatformEnum) }
        }
    }

    @MainThread
    private fun cancelPendingLoads() {
        drainAdmobLoadListeners().forEach {
            safeListenerCall { it.onError(AdErrorMode.MANAGER, DISABLED_LOAD_MESSAGE, AdPlatformTypeEnum.ADMOB) }
        }
        drainApplovinLoadListeners().forEach {
            safeListenerCall { it.onError(AdErrorMode.MANAGER, DISABLED_LOAD_MESSAGE, AdPlatformTypeEnum.APPLOVIN) }
        }
    }

    private fun drainAdmobLoadListeners(): List<AdPlatformLoadListener> {
        return pendingAdmobLoadListeners.toList().also { pendingAdmobLoadListeners.clear() }
    }

    private fun drainApplovinLoadListeners(): List<AdPlatformLoadListener> {
        return pendingApplovinLoadListeners.toList().also { pendingApplovinLoadListeners.clear() }
    }

    private fun notifyShowError(
        listener: AdPlatformShowListener?,
        message: String,
        platform: AdPlatformTypeEnum?
    ) {
        safeListenerCall { globalShowListener?.onError(AdErrorMode.MANAGER, message, platform) }
        safeListenerCall { listener?.onError(AdErrorMode.MANAGER, message, platform) }
    }

    private fun notifyLoadError(
        listener: AdPlatformLoadListener?,
        message: String,
        platform: AdPlatformTypeEnum?
    ) {
        safeListenerCall { globalLoadListener?.onError(AdErrorMode.MANAGER, message, platform) }
        safeListenerCall { listener?.onError(AdErrorMode.MANAGER, message, platform) }
    }

    private fun recordShowTime() {
        lastShowElapsedRealtime = SystemClock.elapsedRealtime()
        lastShowDate = Date()
    }

    private fun isAdmobAdLoaded(): Boolean {
        return admobAppOpenAd != null && AppOpenAdPolicy.wasLoadedRecently(
            admobLoadElapsedRealtime,
            adValidityDurationMillis,
            SystemClock.elapsedRealtime()
        )
    }

    private fun isApplovinAdLoaded(): Boolean {
        return applovinAppOpenAd?.isReady == true && AppOpenAdPolicy.wasLoadedRecently(
            applovinLoadElapsedRealtime,
            adValidityDurationMillis,
            SystemClock.elapsedRealtime()
        )
    }

    private fun isPlatformConfigured(platformName: String): Boolean {
        return when (platformName) {
            ADMOB -> admobPlacementId.isNotBlank()
            APPLOVIN -> applovinPlacementId.isNotBlank() && applovinAppOpenAd != null
            else -> false
        }
    }

    private fun normalizeShowOrder(showOrder: String): List<String> {
        return showOrder.split(',')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun runOnMain(action: () -> Unit) {
        if (isMainThread()) action() else mainHandler.post(action)
    }

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private fun safeListenerCall(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            Log.e(TAG, "App open listener failed", error)
        }
    }

    private companion object {
        const val TAG = "AppOpenAdManager"
        const val ADMOB = "admob"
        const val APPLOVIN = "applovin"
        const val DEFAULT_AD_VALIDITY_DURATION_MILLIS = 4 * 60 * 60 * 1_000L
        const val DISABLED_LOAD_MESSAGE = "App open ads are disabled; pending load was cancelled"
    }
}

internal object AppOpenAdPolicy {
    fun hasShowIntervalElapsed(lastShowMillis: Long?, minimumSeconds: Int, nowMillis: Long): Boolean {
        if (lastShowMillis == null) return true
        val minimumIntervalMillis = minimumSeconds.coerceAtLeast(0) * 1_000L
        val elapsedMillis = nowMillis - lastShowMillis
        return elapsedMillis >= minimumIntervalMillis
    }

    fun wasLoadedRecently(loadMillis: Long, validityMillis: Long, nowMillis: Long): Boolean {
        if (loadMillis <= 0L) return false
        val elapsedMillis = nowMillis - loadMillis
        return elapsedMillis in 0 until validityMillis
    }
}
