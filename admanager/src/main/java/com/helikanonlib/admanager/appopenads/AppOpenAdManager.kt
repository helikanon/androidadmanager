package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import java.util.Date
import java.util.Locale

enum class AppOpenAdShowResult {
    SHOW_REQUESTED,
    QUEUED_ON_MAIN_THREAD,
    DISABLED,
    SHOWING_PAUSED,
    FULL_SCREEN_AD_ACTIVE,
    FULL_SCREEN_AD_INTERVAL_NOT_ELAPSED,
    ALREADY_SHOWING,
    INVALID_ACTIVITY,
    ACTIVITY_EXCLUDED,
    INTERVAL_NOT_ELAPSED,
    AD_NOT_READY,
    NO_CONFIGURED_PLATFORM,
    REQUEST_FAILED
}

class AppOpenAdManager private constructor(
    placements: Map<AdPlatformTypeEnum, String>,
    showOrderStr: String,
    var globalShowListener: AdPlatformShowListener?,
    var globalLoadListener: AdPlatformLoadListener?,
    adValidityDurationMillis: Long,
    adapters: List<AppOpenAdAdapter>,
    private val runtime: AppOpenAdRuntime,
    private val displayState: AppOpenAdDisplayState
) {
    constructor(
        application: Application,
        placements: Map<AdPlatformTypeEnum, String> = emptyMap(),
        showOrderStr: String = "admob",
        globalShowListener: AdPlatformShowListener? = null,
        globalLoadListener: AdPlatformLoadListener? = null,
        adValidityDurationMillis: Long = DEFAULT_AD_VALIDITY_DURATION_MILLIS
    ) : this(
        placements = placements.toMap(),
        showOrderStr = showOrderStr,
        globalShowListener = globalShowListener,
        globalLoadListener = globalLoadListener,
        adValidityDurationMillis = adValidityDurationMillis,
        adapters = createDefaultAdapters(application, placements),
        runtime = AndroidAppOpenAdRuntime(),
        displayState = AppOpenAdDisplayGate
    )

    internal constructor(
        adapters: List<AppOpenAdAdapter>,
        runtime: AppOpenAdRuntime,
        showOrderStr: String,
        globalShowListener: AdPlatformShowListener? = null,
        globalLoadListener: AdPlatformLoadListener? = null,
        adValidityDurationMillis: Long = DEFAULT_AD_VALIDITY_DURATION_MILLIS,
        displayState: AppOpenAdDisplayState = AppOpenAdDisplayGate
    ) : this(
        placements = emptyMap(),
        showOrderStr = showOrderStr,
        globalShowListener = globalShowListener,
        globalLoadListener = globalLoadListener,
        adValidityDurationMillis = adValidityDurationMillis,
        adapters = adapters,
        runtime = runtime,
        displayState = displayState
    )

    val placements: Map<AdPlatformTypeEnum, String> = placements.toMap()
    val showOrderStr: String = normalizeShowOrder(showOrderStr).joinToString(",") { platformName(it) }
    val adValidityDurationMillis: Long = adValidityDurationMillis.also {
        require(it > 0L) { "adValidityDurationMillis must be greater than zero" }
    }

    private val adaptersByPlatform = adapters.associateBy(AppOpenAdAdapter::platform).also {
        require(it.size == adapters.size) { "Only one app open adapter can be registered per platform" }
    }
    private val defaultShowOrder = normalizeShowOrder(this.showOrderStr)
    private val loadingPlatforms = mutableSetOf<AdPlatformTypeEnum>()
    private val loadGenerationByPlatform = adaptersByPlatform.keys.associateWith { 0 }.toMutableMap()
    private val pendingLoadListeners = adaptersByPlatform.keys
        .associateWith { mutableListOf<AdPlatformLoadListener>() }
        .toMutableMap()

    private var isShowing = false
    private var lastShowElapsedRealtime: Long? = null

    var lastShowDate: Date? = null
        private set

    var minElapsedSecondsToNextShow = 10

    var minElapsedSecondsAfterFullScreenAd = 5

    var isEnabled = true
        private set

    var isShowingEnabled = true
        private set

    var excludedActivities = arrayListOf<String>()

    @CheckResult
    @JvmOverloads
    fun show(activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(defaultShowOrder, activity, listener, checkIntervalAndExclusion = false)
    }

    @CheckResult
    @JvmOverloads
    fun show(showOrder: String, activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(normalizeShowOrder(showOrder), activity, listener, checkIntervalAndExclusion = false)
    }

    @CheckResult
    @JvmOverloads
    fun showIntervalElapsed(activity: Activity, listener: AdPlatformShowListener? = null): AppOpenAdShowResult {
        return show(defaultShowOrder, activity, listener, checkIntervalAndExclusion = true)
    }

    private fun show(
        showOrder: List<AdPlatformTypeEnum>,
        activity: Activity,
        listener: AdPlatformShowListener?,
        checkIntervalAndExclusion: Boolean
    ): AppOpenAdShowResult {
        if (!runtime.isMainThread()) {
            runtime.post { show(showOrder, activity, listener, checkIntervalAndExclusion) }
            return AppOpenAdShowResult.QUEUED_ON_MAIN_THREAD
        }

        if (!isEnabled) return AppOpenAdShowResult.DISABLED
        if (!isShowingEnabled) return AppOpenAdShowResult.SHOWING_PAUSED
        if (displayState.isBlocked) return AppOpenAdShowResult.FULL_SCREEN_AD_ACTIVE
        if (!hasFullScreenAdIntervalElapsed()) {
            return AppOpenAdShowResult.FULL_SCREEN_AD_INTERVAL_NOT_ELAPSED
        }
        if (AppOpenAdPolicy.isFullScreenAdActivity(activity.javaClass.name)) {
            return AppOpenAdShowResult.FULL_SCREEN_AD_ACTIVE
        }
        if (activity.isFinishing || activity.isDestroyed) return AppOpenAdShowResult.INVALID_ACTIVITY
        if (isShowing) return AppOpenAdShowResult.ALREADY_SHOWING

        if (checkIntervalAndExclusion) {
            if (isActivityExcluded(activity)) return AppOpenAdShowResult.ACTIVITY_EXCLUDED
            if (!hasShowIntervalElapsed()) return AppOpenAdShowResult.INTERVAL_NOT_ELAPSED
        }

        val configuredOrder = showOrder.filter(adaptersByPlatform::containsKey)
        if (configuredOrder.isEmpty()) {
            notifyShowError(listener, "No app open placement is configured for the show order", null)
            return AppOpenAdShowResult.NO_CONFIGURED_PLATFORM
        }

        val adapter = configuredOrder
            .mapNotNull(adaptersByPlatform::get)
            .firstOrNull { it.isReady(adValidityDurationMillis, runtime.elapsedRealtime()) }

        if (adapter == null) {
            load(configuredOrder, null)
            notifyShowError(listener, "App open ad is not ready; a load was requested", null)
            return AppOpenAdShowResult.AD_NOT_READY
        }

        isShowing = true
        val requested = try {
            adapter.show(activity, createDisplayCallback(adapter.platform, listener, configuredOrder))
        } catch (error: Exception) {
            handleDisplayError(adapter.platform, listener, configuredOrder, error.message.orEmpty())
            false
        }

        if (!requested) {
            isShowing = false
            return AppOpenAdShowResult.REQUEST_FAILED
        }
        return AppOpenAdShowResult.SHOW_REQUESTED
    }

    fun isActivityExcluded(activity: Activity): Boolean {
        return activity.javaClass.simpleName in excludedActivities || activity.javaClass.name in excludedActivities
    }

    fun hasShowIntervalElapsed(nowElapsedRealtime: Long = runtime.elapsedRealtime()): Boolean {
        return AppOpenAdPolicy.hasShowIntervalElapsed(
            lastShowElapsedRealtime,
            minElapsedSecondsToNextShow,
            nowElapsedRealtime
        )
    }

    fun hasFullScreenAdIntervalElapsed(nowElapsedRealtime: Long = runtime.elapsedRealtime()): Boolean {
        return AppOpenAdPolicy.hasShowIntervalElapsed(
            displayState.lastFullScreenAdClosedElapsedRealtime,
            minElapsedSecondsAfterFullScreenAd,
            nowElapsedRealtime
        )
    }

    @JvmOverloads
    fun load(listener: AdPlatformLoadListener? = null) {
        runOnMain { load(defaultShowOrder, listener) }
    }

    @MainThread
    private fun load(showOrder: List<AdPlatformTypeEnum>, listener: AdPlatformLoadListener?) {
        if (!isEnabled) {
            notifyLoadError(listener, DISABLED_LOAD_MESSAGE, null)
            return
        }

        val adapters = showOrder.distinct().mapNotNull(adaptersByPlatform::get)
        if (adapters.isEmpty()) {
            notifyLoadError(listener, "No app open placement is configured for the show order", null)
            return
        }

        val forwardingListener = createLoadListener(listener, adapters.map { it.platform })
        adapters.forEach { enqueueLoad(it, forwardingListener) }
    }

    fun disable() {
        runOnMain {
            if (!isEnabled) return@runOnMain
            isEnabled = false
            adaptersByPlatform.keys.forEach { platform ->
                loadGenerationByPlatform[platform] = loadGeneration(platform) + 1
            }
            cancelPendingLoads()
        }
    }

    fun enable() {
        runOnMain { isEnabled = true }
    }

    fun pauseShowing() {
        runOnMain { isShowingEnabled = false }
    }

    fun resumeShowing() {
        runOnMain { isShowingEnabled = true }
    }

    @MainThread
    private fun enqueueLoad(adapter: AppOpenAdAdapter, listener: AdPlatformLoadListener) {
        if (adapter.isReady(adValidityDurationMillis, runtime.elapsedRealtime())) {
            safeListenerCall { listener.onLoaded(adapter.platform) }
            return
        }

        pendingListeners(adapter.platform).add(listener)
        if (adapter.platform !in loadingPlatforms) startLoad(adapter)
    }

    @MainThread
    private fun startLoad(adapter: AppOpenAdAdapter) {
        val platform = adapter.platform
        if (!isEnabled || platform in loadingPlatforms || pendingListeners(platform).isEmpty()) return

        loadingPlatforms.add(platform)
        val requestGeneration = loadGeneration(platform)
        try {
            adapter.load(object : AppOpenAdLoadCallback {
                override fun onLoaded() {
                    runOnMain { handleLoaded(adapter, requestGeneration) }
                }

                override fun onError(message: String) {
                    runOnMain { handleLoadFailed(adapter, requestGeneration, message) }
                }
            })
        } catch (error: Exception) {
            handleLoadFailed(adapter, requestGeneration, error.message.orEmpty())
        }
    }

    @MainThread
    private fun handleLoaded(adapter: AppOpenAdAdapter, requestGeneration: Int) {
        val platform = adapter.platform
        loadingPlatforms.remove(platform)
        if (requestGeneration != loadGeneration(platform) || !isEnabled) {
            adapter.invalidate()
            restartLoadIfNeeded(adapter)
            return
        }

        drainLoadListeners(platform).forEach {
            safeListenerCall { it.onLoaded(platform) }
        }
    }

    @MainThread
    private fun handleLoadFailed(adapter: AppOpenAdAdapter, requestGeneration: Int, message: String) {
        val platform = adapter.platform
        loadingPlatforms.remove(platform)
        if (requestGeneration != loadGeneration(platform) || !isEnabled) {
            restartLoadIfNeeded(adapter)
            return
        }

        drainLoadListeners(platform).forEach {
            safeListenerCall {
                it.onPlatformError(
                    AdPlatformError(AdFormatEnum.APP_OPEN, platform, 0, message)
                )
            }
        }
    }

    @MainThread
    private fun restartLoadIfNeeded(adapter: AppOpenAdAdapter) {
        if (isEnabled && pendingListeners(adapter.platform).isNotEmpty()) startLoad(adapter)
    }

    private fun createDisplayCallback(
        platform: AdPlatformTypeEnum,
        listener: AdPlatformShowListener?,
        showOrder: List<AdPlatformTypeEnum>
    ) = object : AppOpenAdDisplayCallback {
        override fun onDisplayed() {
            runOnMain {
                recordShowTime()
                safeListenerCall { globalShowListener?.onDisplayed(platform) }
                safeListenerCall { listener?.onDisplayed(platform) }
            }
        }

        override fun onClicked() {
            runOnMain {
                safeListenerCall { globalShowListener?.onClicked(platform) }
                safeListenerCall { listener?.onClicked(platform) }
            }
        }

        override fun onClosed() {
            runOnMain {
                isShowing = false
                recordShowTime()
                safeListenerCall { globalShowListener?.onClosed(platform) }
                safeListenerCall { listener?.onClosed(platform) }
                load(showOrder, null)
            }
        }

        override fun onError(message: String) {
            runOnMain { handleDisplayError(platform, listener, showOrder, message) }
        }
    }

    @MainThread
    private fun handleDisplayError(
        platform: AdPlatformTypeEnum,
        listener: AdPlatformShowListener?,
        showOrder: List<AdPlatformTypeEnum>,
        message: String
    ) {
        isShowing = false
        val platformError = AdPlatformError(AdFormatEnum.APP_OPEN, platform, 0, message)
        val managerError = AdManagerError(
            AdFormatEnum.APP_OPEN,
            0,
            listOf(platform),
            listOf(platformError),
            "App open ad show failed for ${platform.name}"
        )
        notifyShowListeners(listener) { it.onPlatformError(platformError) }
        notifyShowListeners(listener) { it.onError(managerError) }
        load(showOrder, null)
    }

    private fun createLoadListener(
        listener: AdPlatformLoadListener?,
        attemptedPlatforms: List<AdPlatformTypeEnum>
    ) = object : AdPlatformLoadListener() {
        private val completedPlatforms = mutableSetOf<AdPlatformTypeEnum>()
        private val platformErrors = mutableListOf<AdPlatformError>()
        private var hasLoaded = false

        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
            hasLoaded = true
            completedPlatforms.add(adPlatformEnum)
            notifyLoadListeners(listener) { it.onLoaded(adPlatformEnum) }
        }

        override fun onPlatformError(error: AdPlatformError) {
            platformErrors.add(error)
            completedPlatforms.add(error.platform)
            notifyLoadListeners(listener) { it.onPlatformError(error) }
            if (completedPlatforms.size == attemptedPlatforms.size && !hasLoaded) {
                notifyLoadListeners(listener) {
                    it.onError(
                        AdManagerError(
                            AdFormatEnum.APP_OPEN,
                            0,
                            attemptedPlatforms,
                            platformErrors.toList(),
                            "No app open ad loaded from configured platforms"
                        )
                    )
                }
            }
        }

        override fun onError(error: AdManagerError) {
            notifyLoadListeners(listener) { it.onError(error) }
        }
    }

    @MainThread
    private fun cancelPendingLoads() {
        val attemptedPlatforms = adaptersByPlatform.keys.toList()
        val pendingListeners = attemptedPlatforms
            .flatMap(::drainLoadListeners)
            .distinct()
        val error = AdManagerError(
            AdFormatEnum.APP_OPEN,
            0,
            attemptedPlatforms,
            message = DISABLED_LOAD_MESSAGE
        )
        pendingListeners.forEach {
            safeListenerCall {
                it.onError(error)
            }
        }
    }

    private fun pendingListeners(platform: AdPlatformTypeEnum): MutableList<AdPlatformLoadListener> {
        return requireNotNull(pendingLoadListeners[platform])
    }

    private fun drainLoadListeners(platform: AdPlatformTypeEnum): List<AdPlatformLoadListener> {
        return pendingListeners(platform).toList().also { pendingListeners(platform).clear() }
    }

    private fun loadGeneration(platform: AdPlatformTypeEnum): Int {
        return loadGenerationByPlatform[platform] ?: 0
    }

    private fun notifyShowError(
        listener: AdPlatformShowListener?,
        message: String,
        platform: AdPlatformTypeEnum?
    ) {
        val error = AdManagerError(
            AdFormatEnum.APP_OPEN,
            0,
            platform?.let(::listOf) ?: emptyList(),
            message = message
        )
        notifyShowListeners(listener) { it.onError(error) }
    }

    private fun notifyLoadError(
        listener: AdPlatformLoadListener?,
        message: String,
        platform: AdPlatformTypeEnum?
    ) {
        val error = AdManagerError(
            AdFormatEnum.APP_OPEN,
            0,
            platform?.let(::listOf) ?: emptyList(),
            message = message
        )
        notifyLoadListeners(listener) { it.onError(error) }
    }

    private fun notifyShowListeners(
        listener: AdPlatformShowListener?,
        notify: (AdPlatformShowListener) -> Unit
    ) {
        globalShowListener?.let { safeListenerCall { notify(it) } }
        if (listener !== globalShowListener) listener?.let { safeListenerCall { notify(it) } }
    }

    private fun notifyLoadListeners(
        listener: AdPlatformLoadListener?,
        notify: (AdPlatformLoadListener) -> Unit
    ) {
        globalLoadListener?.let { safeListenerCall { notify(it) } }
        if (listener !== globalLoadListener) listener?.let { safeListenerCall { notify(it) } }
    }

    private fun recordShowTime() {
        lastShowElapsedRealtime = runtime.elapsedRealtime()
        lastShowDate = Date()
    }

    private fun normalizeShowOrder(showOrder: String): List<AdPlatformTypeEnum> {
        return showOrder.split(',')
            .mapNotNull { requestedName ->
                AdPlatformTypeEnum.entries.firstOrNull {
                    platformName(it) == requestedName.trim().lowercase(Locale.ROOT)
                }
            }
            .distinct()
    }

    private fun runOnMain(action: () -> Unit) {
        if (runtime.isMainThread()) action() else runtime.post(action)
    }

    private fun safeListenerCall(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            Log.e(TAG, "App open listener failed", error)
        }
    }

    private companion object {
        const val TAG = "AppOpenAdManager"
        const val DEFAULT_AD_VALIDITY_DURATION_MILLIS = 4 * 60 * 60 * 1_000L
        const val DISABLED_LOAD_MESSAGE = "App open ads are disabled; pending load was cancelled"

        fun platformName(platform: AdPlatformTypeEnum): String = platform.name.lowercase(Locale.ROOT)

        fun createDefaultAdapters(
            application: Application,
            placements: Map<AdPlatformTypeEnum, String>
        ): List<AppOpenAdAdapter> = buildList {
            placements[AdPlatformTypeEnum.ADMOB]
                ?.takeIf(String::isNotBlank)
                ?.let { add(AdmobAppOpenAdAdapter(application, it)) }

            placements[AdPlatformTypeEnum.APPLOVIN]
                ?.takeIf(String::isNotBlank)
                ?.let { add(ApplovinAppOpenAdAdapter(it)) }
        }
    }
}

internal object AppOpenAdPolicy {
    private val fullScreenAdActivityNames = setOf(
        "com.google.android.gms.ads.AdActivity",
        "com.applovin.sdk.AppLovinWebViewActivity",
        "com.facebook.ads.AudienceNetworkActivity"
    )

    private val fullScreenAdActivityPackagePrefixes = listOf(
        "com.applovin.adview.",
        "com.bytedance.sdk.openadsdk.activity.",
        "com.fyber.inneractive.sdk.activities.",
        "com.inmobi.ads.rendering.",
        "com.mbridge.msdk.",
        "com.unity3d.ads.adplayer.",
        "com.unity3d.services.ads.adunit.",
        "com.vungle.ads.internal.ui."
    )

    fun isFullScreenAdActivity(activityClassName: String): Boolean {
        return activityClassName in fullScreenAdActivityNames ||
            fullScreenAdActivityPackagePrefixes.any(activityClassName::startsWith)
    }

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
