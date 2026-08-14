package com.helikanonlib.admanager


import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.applovin.sdk.AppLovinSdk
import java.util.*
import java.util.concurrent.TimeUnit


class AdManager {

    companion object {
        var DEFAULT_PLACEMENT_GROUPCODE = "default"
        var DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME = "defaultPlace"
        var DEFAULT_BANNER_SHOWN_WHERE_NAME = "emptyPlace"
        var DEFAULT_MREC_SHOWN_WHERE_NAME = "emptyPlace"
        var DEFAULT_REWARDED_SHOWN_WHERE_NAME = "emptyPlace"
    }

    var testMode: Boolean = false
    var deviceId: String = ""
    var showAds: Boolean = true
    var autoLoadForInterstitial: Boolean = false
    var autoLoadForRewarded: Boolean = true
    var autoLoadDelay: Long = 10
        set(value) {
            require(value >= 0) { "autoLoadDelay cannot be negative" }
            field = value
        }
    var randomInterval: Int = 40
        set(value) {
            require(value >= 0) { "randomInterval cannot be negative" }
            field = value
        }
    var interstitialMinElapsedSecondsToNextShow: Int = 40
        set(value) {
            require(value >= 0) { "interstitialMinElapsedSecondsToNextShow cannot be negative" }
            field = value
        }
    var rewardedMinElapsedSecondsToNextShow: Int = 40
        set(value) {
            require(value >= 0) { "rewardedMinElapsedSecondsToNextShow cannot be negative" }
            field = value
        }
    var isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = false

    private val adPlatforms = mutableListOf<AdPlatformModel>()

    var globalInterstitialShowListener: AdPlatformShowListener? = null
    var globalRewardedShowListener: AdPlatformShowListener? = null
    var globalInterstitialLoadListener: AdPlatformLoadListener? = null
    var globalRewardedLoadListener: AdPlatformLoadListener? = null

    private inline fun notifyLoadListeners(
        globalListener: AdPlatformLoadListener?,
        localListener: AdPlatformLoadListener?,
        notify: (AdPlatformLoadListener) -> Unit
    ) {
        globalListener?.let(notify)
        if (localListener !== globalListener) localListener?.let(notify)
    }

    private inline fun notifyShowListeners(
        globalListener: AdPlatformShowListener?,
        localListener: AdPlatformShowListener?,
        notify: (AdPlatformShowListener) -> Unit
    ) {
        globalListener?.let(notify)
        if (localListener !== globalListener) localListener?.let(notify)
    }

    private fun adsDisabledError(adFormat: AdFormatEnum, placementGroupIndex: Int) =
        AdManagerError(
            format = adFormat,
            placementGroupIndex = placementGroupIndex,
            message = "Ads are disabled because showAds is false"
        )

    private fun notifyLoadDisabled(
        adFormat: AdFormatEnum,
        placementGroupIndex: Int,
        globalListener: AdPlatformLoadListener?,
        localListener: AdPlatformLoadListener?
    ) {
        val error = adsDisabledError(adFormat, placementGroupIndex)
        notifyLoadListeners(globalListener, localListener) { it.onError(error) }
    }

    private fun notifyShowDisabled(
        adFormat: AdFormatEnum,
        placementGroupIndex: Int,
        globalListener: AdPlatformShowListener?,
        localListener: AdPlatformShowListener?
    ) {
        val error = adsDisabledError(adFormat, placementGroupIndex)
        notifyShowListeners(globalListener, localListener) { it.onError(error) }
    }

    private val adPlatformSortByAdFormat = mutableMapOf<String, List<AdPlatformTypeEnum>>()
    private val placementGroups = mutableListOf<String>()

    // handlers
    private var handlerThread: HandlerThread? = null
    private var autoloadInterstitialHandler: Handler? = null
    private var autoloadRewardedHandler: Handler? = null
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    // Keep leases reachable outside SDK callbacks so Activity/manager destruction can unblock App Open ads.
    private val displayLeaseLock = Any()
    private val displayLeasesByActivity = IdentityHashMap<Activity, MutableSet<AppOpenAdDisplayGate.Lease>>()

    private val lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

    constructor() {
        initHandlers()
    }

    private fun initHandlers() {
        if (handlerThread == null) {
            handlerThread = HandlerThread("admanager-bg-thread")
            handlerThread?.start()

            hasWorkingAutoloadInterstitialHandler = false
            hasWorkingAutoloadRewardedHandler = false
            autoloadInterstitialHandler = Handler(handlerThread!!.looper)
            autoloadRewardedHandler = Handler(handlerThread!!.looper)
        }
    }

    private fun acquireFullScreenDisplayLease(activity: Activity): AppOpenAdDisplayGate.Lease {
        return synchronized(displayLeaseLock) {
            AppOpenAdDisplayGate.acquire().also { lease ->
                displayLeasesByActivity.getOrPut(activity, ::mutableSetOf).add(lease)
            }
        }
    }

    private fun releaseFullScreenDisplayLease(activity: Activity, lease: AppOpenAdDisplayGate.Lease) {
        synchronized(displayLeaseLock) {
            displayLeasesByActivity[activity]?.let { activityLeases ->
                activityLeases.remove(lease)
                if (activityLeases.isEmpty()) displayLeasesByActivity.remove(activity)
            }
        }
        lease.release()
    }

    private fun releaseFullScreenDisplayLeases(activity: Activity) {
        val leases = synchronized(displayLeaseLock) {
            displayLeasesByActivity.remove(activity)?.toList().orEmpty()
        }
        leases.forEach { it.release() }
    }

    private fun releaseAllFullScreenDisplayLeases() {
        val leases = synchronized(displayLeaseLock) {
            displayLeasesByActivity.values.flatten().also { displayLeasesByActivity.clear() }
        }
        leases.forEach { it.release() }
    }

    fun initializePlatformsWithActivity(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.initialize(activity, { it ->

            }, testMode)

            if (testMode) {
                platform.platformInstance.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }

    @JvmOverloads
    fun initializePlatforms(
        context: Context,
        onInitializeComplete: (AdInitializationResult) -> Unit,
        onPlatformInitializeComplete: (AdPlatformInitializationResult) -> Unit = {}
    ) {
        if (!showAds) {
            onInitializeComplete(
                AdInitializationResult(
                    AdInitializationStatus.DISABLED,
                    message = "Ad initialization is disabled because showAds is false"
                )
            )
            return
        }

        val platforms = adPlatforms.toList()
        if (platforms.isEmpty()) {
            onInitializeComplete(
                AdInitializationResult(
                    AdInitializationStatus.FAILURE,
                    message = "No ad platform is configured"
                )
            )
            return
        }

        val resultLock = Any()
        val platformResults = mutableListOf<AdPlatformInitializationResult>()
        var completedCount = 0
        var successCount = 0

        platforms.forEach { platform ->
            val onPlatformComplete: (Boolean) -> Unit = { isSuccessful ->
                val platformResult = AdPlatformInitializationResult(
                    platform.platformInstance.platform,
                    isSuccessful
                )

                synchronized(resultLock) {
                    completedCount++
                    if (isSuccessful) successCount++
                    platformResults.add(platformResult)
                    onPlatformInitializeComplete(platformResult)

                    if (completedCount == platforms.size) {
                        val status = when (successCount) {
                            platforms.size -> AdInitializationStatus.SUCCESS
                            0 -> AdInitializationStatus.FAILURE
                            else -> AdInitializationStatus.PARTIAL_SUCCESS
                        }
                        onInitializeComplete(
                            AdInitializationResult(status, platformResults.toList())
                        )
                    }
                }
            }

            try {
                platform.platformInstance.initialize(
                    context,
                    onPlatformComplete,
                    testMode
                )
            } catch (error: Exception) {
                onPlatformComplete(false)
            }

            if (testMode) {
                platform.platformInstance.enableTestMode(context, deviceId)
            }
        }
    }

    fun isAllPlatformsSdksInitialized(): Boolean =
        adPlatforms.isNotEmpty() && adPlatforms.all { it.platformInstance.isInitialized }

    fun isPlatformSdkInitialized(platform: AdPlatformModel): Boolean =
        platform.platformInstance.isInitialized

    fun enableTestMode(activity: Activity, deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        adPlatforms.forEach forEach@{ platform ->
            if (testMode) {
                platform.platformInstance.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }


    var isEnableShowLoadingViewForInterstitial = true
    private var loadingView: AdsLoadingCustomView? = null

    private fun clearLoadingView(activity: Activity? = null) {
        val view = loadingView ?: return
        if (activity != null && view.context !== activity) return

        (view.parent as? ViewGroup)?.removeView(view)
        loadingView = null
    }

    fun initLoadingView(activity: Activity, rootView: ViewGroup? = null) {
        // Never reuse a View created with another Activity context.
        clearLoadingView()
        loadingView = AdsLoadingCustomView(activity, null)

        rootView?.let {
            setLoadingLayoutParams(rootView)
        }

        loadingView?.bringToFront()
        // loadingView?.rootView?.visibility = View.VISIBLE
    }

    fun setLoadingLayoutParams(rootView: ViewGroup) {
        var className = rootView::class.simpleName

        if (rootView::class.simpleName == "NestedScrollView") {
            className = rootView.getChildAt(0)::class.simpleName
        }

        when (className) {
            "RelativeLayout" -> {
                val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)

                loadingView?.layoutParams = layoutParams
            }

            "ConstraintLayout" -> {
                val layoutParams: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.bottomToBottom = ConstraintSet.PARENT_ID
                layoutParams.endToEnd = ConstraintSet.PARENT_ID
                layoutParams.startToStart = ConstraintSet.PARENT_ID
                layoutParams.topToTop = ConstraintSet.PARENT_ID

                loadingView?.layoutParams = layoutParams
            }

            "LinearLayout" -> {
                val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.gravity = Gravity.CENTER

                loadingView?.layoutParams = layoutParams
            }
        }
    }

    fun addLoadingViewToActivity(activity: Activity) {
        if (!isEnableShowLoadingViewForInterstitial) {
            return
        }

        try {

            val activityRootView = activity.findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0) as ViewGroup

            initLoadingView(activity, activityRootView)

            var className = activityRootView::class.simpleName

            if (className == "NestedScrollView") {
                (activityRootView.getChildAt(0) as ViewGroup).addView(loadingView)
            } else {
                activityRootView.addView(loadingView)
            }

            ViewCompat.setTranslationZ(loadingView as View, 100f);

            loadingView?.bringToFront()
            //loadingView?.invalidate()
        } catch (e: Exception) {
            clearLoadingView(activity)
            e.printStackTrace()
        }
    }

    fun removeLoadingViewFromActivity(activity: Activity) {
        try {
            clearLoadingView(activity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setPlacementGroups(placementGroups: List<String>) = apply {
        require(placementGroups.isNotEmpty()) { "At least one placement group must be configured" }
        require(placementGroups.none { it.isBlank() }) { "Placement group names cannot be blank" }
        require(placementGroups.distinct().size == placementGroups.size) {
            "Placement group names must be unique: $placementGroups"
        }

        this.placementGroups.clear()
        this.placementGroups.addAll(placementGroups)
    }

    fun setAdPlatforms(adPlatforms: List<AdPlatformModel>) = apply {
        this.adPlatforms.clear()
        this.adPlatforms.addAll(adPlatforms)
    }

    fun addAdPlatform(adPlatform: AdPlatformModel) = apply { adPlatforms.add(adPlatform) }
    fun getAdPlatformByType(platformType: AdPlatformTypeEnum): AdPlatformModel? {
        val filteredPlatforms = adPlatforms.filter { it -> it.platformInstance.platform == platformType }
        return if (filteredPlatforms.size > 0) filteredPlatforms[0] else null
    }


    fun getPlacementGroupIndexByName(pgName: String): Int {
        val placementGroupIndex = placementGroups.indexOf(pgName)
        require(placementGroupIndex >= 0) {
            "Unknown placement group '$pgName'. Configured groups: $placementGroups"
        }
        return placementGroupIndex
    }

    fun getPlacementGroupNameByIndex(pgIndex: Int): String {
        require(pgIndex in placementGroups.indices) {
            "Invalid placement group index $pgIndex. Configured groups: $placementGroups"
        }
        return placementGroups[pgIndex]
    }

    private fun validatePlacementGroup(placementGroupIndex: Int): String {
        val placementGroupName = getPlacementGroupNameByIndex(placementGroupIndex)

        adPlatforms.forEach { platform ->
            val platformPlacementGroups = platform.platformInstance.placementGroups
            val platformName = platform.platformInstance.platform.name

            require(placementGroupIndex in platformPlacementGroups.indices) {
                "Placement group '$placementGroupName' at index $placementGroupIndex is not configured for $platformName"
            }
            require(platformPlacementGroups[placementGroupIndex].groupName == placementGroupName) {
                "Placement group mismatch for $platformName at index $placementGroupIndex: " +
                    "expected '$placementGroupName', found '${platformPlacementGroups[placementGroupIndex].groupName}'"
            }
        }

        return placementGroupName
    }

    fun setAdPlatformOrder(
        placementGroupIndex: Int,
        adFormat: AdFormatEnum,
        vararg platforms: AdPlatformTypeEnum
    ): AdPlatformOrderResult {
        val supportedFormats = setOf(
            AdFormatEnum.INTERSTITIAL,
            AdFormatEnum.BANNER,
            AdFormatEnum.REWARDED,
            AdFormatEnum.MREC,
            AdFormatEnum.NATIVE,
            AdFormatEnum.NATIVE_MEDIUM
        )
        if (adFormat !in supportedFormats) {
            return AdPlatformOrderResult(false, "Platform order is not supported for ${adFormat.name}")
        }
        if (platforms.isEmpty()) {
            return AdPlatformOrderResult(false, "Platform order cannot be empty")
        }
        if (platforms.distinct().size != platforms.size) {
            return AdPlatformOrderResult(false, "Platform order contains duplicate platforms: ${platforms.toList()}")
        }

        val placementGroupName = try {
            validatePlacementGroup(placementGroupIndex)
        } catch (error: IllegalArgumentException) {
            return AdPlatformOrderResult(false, error.message)
        }

        platforms.forEach { platformType ->
            val platform = getAdPlatformByType(platformType)
                ?: return AdPlatformOrderResult(false, "$platformType is not configured in AdManager")

            if (!platform.isEnabledFor(adFormat)) {
                return AdPlatformOrderResult(false, "$platformType is not enabled for ${adFormat.name}")
            }
        }

        adPlatformSortByAdFormat[adFormat.name + "__" + placementGroupName] = platforms.toList()
        return AdPlatformOrderResult(true)
    }

    fun setAdPlatformSortByAdFormatStr(
        placementGroupIndex: Int,
        adFormatName: String,
        adPlatformsStr: String
    ): AdPlatformOrderResult {
        val adFormat = AdFormatEnum.entries.firstOrNull {
            it.name.equals(adFormatName.trim(), ignoreCase = true)
        } ?: return AdPlatformOrderResult(false, "Unknown ad format '$adFormatName'")

        val platformNames = adPlatformsStr.split(',').map { it.trim() }
        if (platformNames.isEmpty() || platformNames.any { it.isEmpty() }) {
            return AdPlatformOrderResult(false, "Platform order contains an empty platform name")
        }

        val platforms = platformNames.map { platformName ->
            AdPlatformTypeEnum.entries.firstOrNull {
                it.name.equals(platformName, ignoreCase = true)
            } ?: return AdPlatformOrderResult(false, "Unknown ad platform '$platformName'")
        }

        return setAdPlatformOrder(placementGroupIndex, adFormat, *platforms.toTypedArray())
    }

    private fun AdPlatformModel.isEnabledFor(adFormat: AdFormatEnum): Boolean = when (adFormat) {
        AdFormatEnum.INTERSTITIAL -> showInterstitial
        AdFormatEnum.BANNER -> showBanner
        AdFormatEnum.REWARDED -> showRewarded
        AdFormatEnum.MREC -> showMrec
        AdFormatEnum.NATIVE, AdFormatEnum.NATIVE_MEDIUM -> showNative
        AdFormatEnum.APP_OPEN -> false
    }


    private fun _getAdPlatformsWithSortedByAdFormat(adFormatEnum: AdFormatEnum, placementGroupIndex: Int): MutableList<AdPlatformModel> {
        val filteredAdPlatforms = mutableListOf<AdPlatformModel>()
        val placementGroupName = validatePlacementGroup(placementGroupIndex)
        val adFormatSort = adPlatformSortByAdFormat[adFormatEnum.name.uppercase(Locale.ENGLISH) + "__" + placementGroupName]

        if (adFormatSort != null) {
            adFormatSort.forEach { adPlatformType ->
                getAdPlatformByType(adPlatformType)?.let { filteredAdPlatforms.add(it) }
            }
        } else {
            adPlatforms.forEach { adPlatform ->
                val isAdd = when (adFormatEnum) {
                    AdFormatEnum.INTERSTITIAL -> adPlatform.showInterstitial
                    AdFormatEnum.BANNER -> adPlatform.showBanner
                    AdFormatEnum.REWARDED -> adPlatform.showRewarded
                    AdFormatEnum.MREC -> adPlatform.showMrec
                    AdFormatEnum.NATIVE -> adPlatform.showNative
                    AdFormatEnum.NATIVE_MEDIUM -> adPlatform.showNative
                    else -> false
                }

                if (isAdd) {
                    filteredAdPlatforms.add(adPlatform)
                }
            }
        }

        return filteredAdPlatforms
    }

    @JvmOverloads
    fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null, parallel: Boolean = false, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.INTERSTITIAL, placementGroupIndex, globalInterstitialLoadListener, listener)
        validatePlacementGroup(placementGroupIndex)

        if (platform == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
                if (interstitialAdPlatforms.isEmpty()) {
                    val error = AdManagerError(
                        format = AdFormatEnum.INTERSTITIAL,
                        placementGroupIndex = placementGroupIndex,
                        message = "No interstitial ad platform is configured for placement group index $placementGroupIndex"
                    )
                    notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(error) }
                    return
                }
                val platformsToLoad = interstitialAdPlatforms.filterNot {
                    it.platformInstance.isInterstitialLoaded(placementGroupIndex)
                }
                if (platformsToLoad.isEmpty()) return

                val platformErrors = mutableListOf<AdPlatformError>()
                var completedCount = 0
                var hasLoaded = false
                platformsToLoad.forEach { candidate ->
                    candidate.platformInstance.loadInterstitial(activity, object : AdPlatformLoadListener() {
                        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                            hasLoaded = true
                            completedCount++
                            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
                        }

                        override fun onPlatformError(error: AdPlatformError) {
                            platformErrors.add(error)
                            completedCount++
                            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }
                            if (completedCount == platformsToLoad.size && !hasLoaded) {
                                val managerError = AdManagerError(
                                    AdFormatEnum.INTERSTITIAL,
                                    placementGroupIndex,
                                    platformsToLoad.map { it.platformInstance.platform },
                                    platformErrors.toList(),
                                    "No interstitial found in all platforms"
                                )
                                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
                            }
                        }
                    }, placementGroupIndex)
                }
            } else {
                _loadInterstitialFromFirstAvailable(activity, listener, 0, placementGroupIndex)
            }
        } else {
            _loadInterstitial(activity, listener = listener, platform = platform, placementGroupIndex)
        }
    }

    private fun _loadInterstitialFromFirstAvailable(
        activity: Activity,
        listener: AdPlatformLoadListener? = null,
        index: Int = 0,
        placementGroupIndex: Int,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
        if (interstitialAdPlatforms.isEmpty()) {
            val error = AdManagerError(
                format = AdFormatEnum.INTERSTITIAL,
                placementGroupIndex = placementGroupIndex,
                message = "No interstitial ad platform is configured for placement group index $placementGroupIndex"
            )
            notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(error) }
            return
        }
        if (index >= interstitialAdPlatforms.size) {
            return
        }
        val platform = interstitialAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }

                if ((index + 1) < interstitialAdPlatforms.size) {
                    activity.runOnUiThread {
                        _loadInterstitialFromFirstAvailable(activity, listener, index + 1, placementGroupIndex, platformErrors)
                    }
                } else {
                    val managerError = AdManagerError(
                        AdFormatEnum.INTERSTITIAL,
                        placementGroupIndex,
                        interstitialAdPlatforms.map { it.platformInstance.platform },
                        platformErrors.toList(),
                        "No interstitial found in all platforms"
                    )
                    notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
                }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }
        }
        platform.platformInstance.loadInterstitial(activity, _listener, placementGroupIndex)
    }

    private fun _loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onPlatformError(error) }
                val managerError = AdManagerError(
                    AdFormatEnum.INTERSTITIAL,
                    placementGroupIndex,
                    listOf(platform.platformInstance.platform),
                    listOf(error),
                    "Interstitial load failed for ${platform.platformInstance.platform.name}"
                )
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onError(managerError) }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalInterstitialLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }

        }
        platform.platformInstance.loadInterstitial(activity, _listener, placementGroupIndex)
    }

    /*
    call _showInterstitial(listener, platform) in onLoaded and onError because of
    we wants call listener?.onError by _showInterstitial
     */
    @JvmOverloads
    fun loadAndShowInterstitial(activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupIndex, globalInterstitialShowListener, listener)
        validatePlacementGroup(placementGroupIndex)

        activity.runOnUiThread {
            addLoadingViewToActivity(activity)
        }
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, false)
            }

            override fun onError(error: AdManagerError) {
                _showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, false)
            }
        }
        loadInterstitial(activity, loadListener, platform, false, placementGroupIndex)
    }

    @JvmOverloads
    fun showInterstitialForTimeStrategy(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ): AdShowRequestResult {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupIndex, globalInterstitialShowListener, listener)
            return AdShowRequestResult.DISABLED
        }
        validatePlacementGroup(placementGroupIndex)

        var isAvailableToShow = true
        val lastShowDate = lastShowDateByAdFormat.get(AdFormatEnum.INTERSTITIAL)

        if (lastShowDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastShowDate.time) / 1000
            val requiredElapsedTime = randInt(0, randomInterval) + interstitialMinElapsedSecondsToNextShow
            isAvailableToShow = elapsedSeconds > requiredElapsedTime
        }

        if (isAvailableToShow) {
            showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, loadAndShowIfNotExistsAdsOnAutoloadMode)
            return AdShowRequestResult.SHOW_REQUESTED
        }

        return AdShowRequestResult.THROTTLED
    }

    @JvmOverloads
    fun showInterstitial(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupIndex, globalInterstitialShowListener, listener)
        validatePlacementGroup(placementGroupIndex)

        if (autoLoadForInterstitial) {
            val isShowed = _showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, loadAndShowIfNotExistsAdsOnAutoloadMode)
        } else {

            activity.runOnUiThread {
                addLoadingViewToActivity(activity)
            }
            loadAndShowInterstitial(activity, shownWhere, listener, platform, placementGroupIndex)
        }
    }

    // load edilmiş reklamı göstermeye çalışır.
    private fun _showInterstitial(
        activity: Activity, shownWhere: String, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int,
        loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = false
    ): Boolean {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.INTERSTITIAL, placementGroupIndex, globalInterstitialShowListener, listener)
            return false
        }

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
        var displayLease: AppOpenAdDisplayGate.Lease? = null

        fun releaseDisplayLease() {
            displayLease?.let { releaseFullScreenDisplayLease(activity, it) }
            displayLease = null
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {
                AppOpenAdDisplayGate.recordFullScreenAdClosed()
                releaseDisplayLease()

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                notifyShowListeners(globalInterstitialShowListener, listener) { it.onClosed(adPlatformEnum) }
                saveLastShowDate(AdFormatEnum.INTERSTITIAL)

                // on close load new one for next show
                if (autoLoadForInterstitial) {
                    _autoloadInterstitialByHandler(activity, null, null)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                stopAutoloadInterstitialHandler()

                notifyShowListeners(globalInterstitialShowListener, listener) { it.onDisplayed(adPlatformEnum) }

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onClicked(adPlatformEnum) }
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onRewarded(type, amount, adPlatformEnum) }
            }

            override fun onPlatformError(error: AdPlatformError) {
                releaseDisplayLease()
                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }
                notifyShowListeners(globalInterstitialShowListener, listener) { it.onPlatformError(error) }

                if (autoLoadForInterstitial) {
                    if (isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                        stopAutoloadInterstitialHandler()
                        loadAndShowInterstitial(activity, shownWhere, listener, platform, placementGroupIndex)
                    } else {
                        _autoloadInterstitialByHandler(activity, null, null)
                        val managerError = AdManagerError(
                            AdFormatEnum.INTERSTITIAL,
                            placementGroupIndex,
                            listOf(error.platform),
                            listOf(error),
                            "Interstitial show failed for ${error.platform.name}"
                        )
                        notifyShowListeners(globalInterstitialShowListener, listener) { it.onError(managerError) }
                    }
                } else {
                    val managerError = AdManagerError(
                        AdFormatEnum.INTERSTITIAL,
                        placementGroupIndex,
                        listOf(error.platform),
                        listOf(error),
                        "Interstitial show failed for ${error.platform.name}"
                    )
                    notifyShowListeners(globalInterstitialShowListener, listener) { it.onError(managerError) }
                }
            }
        }

        fun showLoadedInterstitial(loadedPlatform: AdPlatformModel): Boolean {
            displayLease = acquireFullScreenDisplayLease(activity)
            return try {
                loadedPlatform.platformInstance.showInterstitial(activity, shownWhere, _listener, placementGroupIndex)
                true
            } catch (error: Exception) {
                releaseDisplayLease()
                _listener.onPlatformError(
                    AdPlatformError(
                        AdFormatEnum.INTERSTITIAL,
                        loadedPlatform.platformInstance.platform,
                        placementGroupIndex,
                        "${loadedPlatform.platformInstance.platform.name} interstitial show failed: ${error.message.orEmpty()}",
                        error
                    )
                )
                true
            }
        }

        var hasLoadedInterstitial = false
        if (platform != null) {
            if (platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                hasLoadedInterstitial = showLoadedInterstitial(platform)
            }
        } else {
            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ platform ->
                    if (platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                        hasLoadedInterstitial = showLoadedInterstitial(platform)
                        return@breaker
                        //return@forEach
                    }
                }
            }
        }

        if (!hasLoadedInterstitial) {
            val attemptedPlatforms = platform?.let { listOf(it.platformInstance.platform) }
                ?: interstitialAdPlatforms.map { it.platformInstance.platform }
            val error = AdManagerError(
                AdFormatEnum.INTERSTITIAL,
                placementGroupIndex,
                attemptedPlatforms,
                message = "There is no loaded interstitial to show"
            )
            activity.runOnUiThread { removeLoadingViewFromActivity(activity) }
            notifyShowListeners(globalInterstitialShowListener, listener) { it.onError(error) }
        }

        return hasLoadedInterstitial
    }

    private var lastPostDelayedSetTimeForInterstitialLoad: Date? = null
    private fun _autoloadInterstitialByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (hasWorkingAutoloadInterstitialHandler) {
            if (lastPostDelayedSetTimeForInterstitialLoad != null) {
                // val diffInMillies = Date().time - lastPostDelayedSetTime!!.time
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForInterstitialLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()

        //stopAutoloadInterstitialHandler()

        hasWorkingAutoloadInterstitialHandler = true
        lastPostDelayedSetTimeForInterstitialLoad = Date()

        autoloadInterstitialHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        // Intentionally reload only the primary placement group to avoid unused ad requests.
                        if (index < 1) {
                            loadInterstitial(activity, listener, platform, false, getPlacementGroupIndexByName(pgName))
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadInterstitialHandler = false
        }, autoLoadDelay * 1000)
    }


    private var lastPostDelayedSetTimeForRewardedLoad: Date? = null
    private fun _autoloadRewardedByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (hasWorkingAutoloadRewardedHandler) {
            if (lastPostDelayedSetTimeForRewardedLoad != null) {
                val diffSeconds = TimeUnit.SECONDS.convert(Date().time - lastPostDelayedSetTimeForRewardedLoad!!.time, TimeUnit.MILLISECONDS)
                if (diffSeconds < (autoLoadDelay + 2)) {
                    return
                }
            }
        }

        initHandlers()
        //stopAutoloadRewardedHandler()

        hasWorkingAutoloadRewardedHandler = true
        lastPostDelayedSetTimeForRewardedLoad = Date()

        autoloadRewardedHandler?.postDelayed({
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        // Intentionally reload only the primary placement group to avoid unused ad requests.
                        if (index > 0) return@forEachIndexed
                        loadRewarded(activity, listener, platform, false, getPlacementGroupIndexByName(pgName))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadRewardedHandler = false
        }, autoLoadDelay * 1000)
    }


    @JvmOverloads
    fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.BANNER, placementGroupIndex, null, listener)
        validatePlacementGroup(placementGroupIndex)


        if (platform == null) {
            var startFrom = 0

            // if already banner loaded, start from this platform
            val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
            if (bannerAdPlatforms.isEmpty()) {
                listener?.onError(
                    AdManagerError(
                        format = AdFormatEnum.BANNER,
                        placementGroupIndex = placementGroupIndex,
                        message = "No banner ad platform is configured for placement group index $placementGroupIndex"
                    )
                )
                return
            }
            run breaker@{
                bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                    if (_platform.platformInstance.isBannerLoaded(placementGroupIndex)) {
                        startFrom = i
                        return@breaker
                    }
                }
            }
            _showBannerFromFirstAvailable(activity, containerView, listener, startFrom, placementGroupIndex)
        } else {
            _showBanner(activity, containerView, listener, platform, placementGroupIndex)
        }
    }

    private fun _showBannerFromFirstAvailable(
        activity: Activity,
        containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null,
        platformIndex: Int = 0,
        placementGroupIndex: Int,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
        if (platformIndex >= bannerAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                listener?.onPlatformError(error)

                if ((platformIndex + 1) < bannerAdPlatforms.size) {
                    activity.runOnUiThread {
                        _showBannerFromFirstAvailable(activity, containerView, listener, platformIndex + 1, placementGroupIndex, platformErrors)
                    }
                } else {
                    listener?.onError(
                        AdManagerError(
                            AdFormatEnum.BANNER,
                            placementGroupIndex,
                            bannerAdPlatforms.map { it.platformInstance.platform },
                            platformErrors.toList(),
                            "No banner found in all platforms"
                        )
                    )
                }
            }
        }

        val platform = bannerAdPlatforms[platformIndex]
        platform.platformInstance.showBanner(activity, containerView, _listener, placementGroupIndex)
    }

    private fun _showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {

        platform.platformInstance.showBanner(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                listener?.onPlatformError(error)
                listener?.onError(
                    AdManagerError(
                        AdFormatEnum.BANNER,
                        placementGroupIndex,
                        listOf(platform.platformInstance.platform),
                        listOf(error),
                        "Banner show failed for ${error.platform.name}"
                    )
                )
            }
        }, placementGroupIndex)
    }

    fun hasLoadedInterstitial(platform: AdPlatformModel? = null, placementGroupIndex: Int): Boolean {
        if (!showAds) return false
        validatePlacementGroup(placementGroupIndex)

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
        return interstitialAdPlatforms.any { candidate ->
            val isRequestedPlatform = platform == null ||
                candidate.platformInstance.platform == platform.platformInstance.platform

            isRequestedPlatform && candidate.platformInstance.isInterstitialLoaded(placementGroupIndex)
        }
    }

    fun hasLoadedRewarded(platform: AdPlatformModel? = null, placementGroupIndex: Int = 0): Boolean {
        if (!showAds) return false
        validatePlacementGroup(placementGroupIndex)

        var hasLoaded = false

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
        run breaker@{
            rewardedAdPlatforms.forEach forEach@{ _platform ->
                if (platform != null && _platform.platformInstance.platform != platform.platformInstance.platform) {
                    return@forEach
                }

                if (_platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                    hasLoaded = true
                    return@breaker
                }
            }
        }

        return hasLoaded
    }

    @JvmOverloads
    fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null, parallel: Boolean = false, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.REWARDED, placementGroupIndex, globalRewardedLoadListener, listener)
        validatePlacementGroup(placementGroupIndex)

        if (platform == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
                if (rewardedAdPlatforms.isEmpty()) {
                    val error = AdManagerError(
                        format = AdFormatEnum.REWARDED,
                        placementGroupIndex = placementGroupIndex,
                        message = "No rewarded ad platform is configured for placement group index $placementGroupIndex"
                    )
                    notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(error) }
                    return
                }
                val platformsToLoad = rewardedAdPlatforms.filterNot {
                    it.platformInstance.isRewardedLoaded(placementGroupIndex)
                }
                if (platformsToLoad.isEmpty()) return

                val platformErrors = mutableListOf<AdPlatformError>()
                var completedCount = 0
                var hasLoaded = false
                platformsToLoad.forEach { candidate ->
                    candidate.platformInstance.loadRewarded(activity, object : AdPlatformLoadListener() {
                        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                            hasLoaded = true
                            completedCount++
                            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }
                        }

                        override fun onPlatformError(error: AdPlatformError) {
                            platformErrors.add(error)
                            completedCount++
                            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }
                            if (completedCount == platformsToLoad.size && !hasLoaded) {
                                val managerError = AdManagerError(
                                    AdFormatEnum.REWARDED,
                                    placementGroupIndex,
                                    platformsToLoad.map { it.platformInstance.platform },
                                    platformErrors.toList(),
                                    "No rewarded found in all platforms"
                                )
                                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
                            }
                        }
                    }, placementGroupIndex)
                }
            } else {
                _loadRewardedFromFirstAvailable(activity, listener, 0, placementGroupIndex)
            }

        } else {
            _loadRewarded(activity, listener, platform, placementGroupIndex)
        }
    }

    private fun _loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }
                val managerError = AdManagerError(
                    AdFormatEnum.REWARDED,
                    placementGroupIndex,
                    listOf(platform.platformInstance.platform),
                    listOf(error),
                    "Rewarded load failed for ${platform.platformInstance.platform.name}"
                )
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }
            }

        }
        platform.platformInstance.loadRewarded(activity, _listener, placementGroupIndex)
    }

    private fun _loadRewardedFromFirstAvailable(
        activity: Activity,
        listener: AdPlatformLoadListener? = null,
        index: Int = 0,
        placementGroupIndex: Int,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
        if (rewardedAdPlatforms.isEmpty()) {
            val error = AdManagerError(
                format = AdFormatEnum.REWARDED,
                placementGroupIndex = placementGroupIndex,
                message = "No rewarded ad platform is configured for placement group index $placementGroupIndex"
            )
            notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(error) }
            return
        }
        if (index >= rewardedAdPlatforms.size) {
            return
        }
        val platform = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onPlatformError(error) }

                if ((index + 1) < rewardedAdPlatforms.size) {
                    activity.runOnUiThread {
                        _loadRewardedFromFirstAvailable(activity, listener, index + 1, placementGroupIndex, platformErrors)
                    }
                } else {
                    val managerError = AdManagerError(
                        AdFormatEnum.REWARDED,
                        placementGroupIndex,
                        rewardedAdPlatforms.map { it.platformInstance.platform },
                        platformErrors.toList(),
                        "No rewarded found in all platforms"
                    )
                    notifyLoadListeners(globalRewardedLoadListener, listener) { it.onError(managerError) }
                }
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                notifyLoadListeners(globalRewardedLoadListener, listener) { it.onLoaded(adPlatformEnum) }

            }
        }

        platform.platformInstance.loadRewarded(activity, _listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadAndShowRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupIndex, globalRewardedShowListener, listener)
        validatePlacementGroup(placementGroupIndex)
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                _showRewarded(activity, listener, platform, placementGroupIndex)
            }

            override fun onError(error: AdManagerError) {
                _showRewarded(activity, listener, platform, placementGroupIndex)
            }
        }
        loadRewarded(activity, loadListener, platform, false, placementGroupIndex)
    }

    @JvmOverloads
    fun showRewarded(
        activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupIndex, globalRewardedShowListener, listener)
        validatePlacementGroup(placementGroupIndex)

        if (autoLoadForRewarded) {
            val isShowed = _showRewarded(activity, listener, platform, placementGroupIndex)

            if (!isShowed) {
                if (isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                    stopAutoloadRewardedHandler()
                    loadAndShowRewarded(activity, listener, platform, placementGroupIndex)
                }
            }
        } else {
            loadAndShowRewarded(activity, listener, platform, placementGroupIndex)
        }
    }

    fun _showRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int): Boolean {
        if (!showAds) {
            notifyShowDisabled(AdFormatEnum.REWARDED, placementGroupIndex, globalRewardedShowListener, listener)
            return false
        }
        validatePlacementGroup(placementGroupIndex)

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
        var displayLease: AppOpenAdDisplayGate.Lease? = null

        fun releaseDisplayLease() {
            displayLease?.let { releaseFullScreenDisplayLease(activity, it) }
            displayLease = null
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum) {
                AppOpenAdDisplayGate.recordFullScreenAdClosed()
                releaseDisplayLease()
                // on close load new one for next show
                notifyShowListeners(globalRewardedShowListener, listener) { it.onClosed(adPlatformEnum) }
                saveLastShowDate(AdFormatEnum.REWARDED)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, null, platform)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                stopAutoloadRewardedHandler()

                notifyShowListeners(globalRewardedShowListener, listener) { it.onDisplayed(adPlatformEnum) }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalRewardedShowListener, listener) { it.onClicked(adPlatformEnum) }
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum) {
                notifyShowListeners(globalRewardedShowListener, listener) { it.onRewarded(type, amount, adPlatformEnum) }
            }

            override fun onPlatformError(error: AdPlatformError) {
                releaseDisplayLease()
                notifyShowListeners(globalRewardedShowListener, listener) { it.onPlatformError(error) }
                val managerError = AdManagerError(
                    AdFormatEnum.REWARDED,
                    placementGroupIndex,
                    listOf(error.platform),
                    listOf(error),
                    "Rewarded show failed for ${error.platform.name}"
                )
                notifyShowListeners(globalRewardedShowListener, listener) { it.onError(managerError) }

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, null, platform)
                }
            }
        }

        fun showLoadedRewarded(loadedPlatform: AdPlatformModel): Boolean {
            displayLease = acquireFullScreenDisplayLease(activity)
            return try {
                loadedPlatform.platformInstance.showRewarded(activity, _listener, placementGroupIndex)
                true
            } catch (error: Exception) {
                releaseDisplayLease()
                _listener.onPlatformError(
                    AdPlatformError(
                        AdFormatEnum.REWARDED,
                        loadedPlatform.platformInstance.platform,
                        placementGroupIndex,
                        "${loadedPlatform.platformInstance.platform.name} rewarded show failed: ${error.message.orEmpty()}",
                        error
                    )
                )
                true
            }
        }

        var hasLoadedRewarded = false

        if (platform != null) {
            if (platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                hasLoadedRewarded = showLoadedRewarded(platform)
            }
        } else {
            run breaker@{
                rewardedAdPlatforms.forEach forEach@{ platform ->
                    if (platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                        hasLoadedRewarded = showLoadedRewarded(platform)
                        return@breaker
                    }
                }
            }

        }

        if (!hasLoadedRewarded) {
            val attemptedPlatforms = platform?.let { listOf(it.platformInstance.platform) }
                ?: rewardedAdPlatforms.map { it.platformInstance.platform }
            val error = AdManagerError(
                AdFormatEnum.REWARDED,
                placementGroupIndex,
                attemptedPlatforms,
                message = "There is no loaded rewarded to show"
            )
            notifyShowListeners(globalRewardedShowListener, listener) { it.onError(error) }
        }

        return hasLoadedRewarded
    }

    @JvmOverloads
    fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyShowDisabled(AdFormatEnum.MREC, placementGroupIndex, null, listener)
        validatePlacementGroup(placementGroupIndex)
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)

        if (mrecAdPlatforms.size == 0) {
            listener?.onError(
                AdManagerError(
                    format = AdFormatEnum.MREC,
                    placementGroupIndex = placementGroupIndex,
                    message = "No MREC ad platform is configured for placement group index $placementGroupIndex"
                )
            )
            return
        }

        if (platform == null) {
            var startFrom = 0

            // if already mrec banner loaded, start from this platform
            run breaker@{
                mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                    if (_platform.platformInstance.isMrecLoaded(placementGroupIndex)) {
                        startFrom = i
                        return@breaker
                    }
                }
            }

            _showMrecFromFirstAvailable(activity, containerView, listener, startFrom, placementGroupIndex)
        } else {
            _showMrec(activity, containerView, listener, platform, placementGroupIndex)
        }
    }

    private fun _showMrecFromFirstAvailable(
        activity: Activity,
        containerView: RelativeLayout,
        listener: AdPlatformShowListener? = null,
        index: Int = 0,
        placementGroupIndex: Int,
        platformErrors: MutableList<AdPlatformError> = mutableListOf()
    ) {
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)
        if (index >= mrecAdPlatforms.size) {
            return
        }
        val platform = mrecAdPlatforms[index]

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                platformErrors.add(error)
                listener?.onPlatformError(error)

                if ((index + 1) < mrecAdPlatforms.size) {
                    activity.runOnUiThread {
                        _showMrecFromFirstAvailable(activity, containerView, listener, index + 1, placementGroupIndex, platformErrors)
                    }
                } else {
                    listener?.onError(
                        AdManagerError(
                            AdFormatEnum.MREC,
                            placementGroupIndex,
                            mrecAdPlatforms.map { it.platformInstance.platform },
                            platformErrors.toList(),
                            "No MREC found in all platforms"
                        )
                    )
                }
            }
        }, placementGroupIndex)
    }

    private fun _showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onPlatformError(error: AdPlatformError) {
                listener?.onPlatformError(error)
                listener?.onError(
                    AdManagerError(
                        AdFormatEnum.MREC,
                        placementGroupIndex,
                        listOf(platform.platformInstance.platform),
                        listOf(error),
                        "MREC show failed for ${error.platform.name}"
                    )
                )
            }
        }, placementGroupIndex)
    }

    private fun saveLastShowDate(adFormatEnum: AdFormatEnum) {
        lastShowDateByAdFormat.put(adFormatEnum, Date())
    }

    fun stopAutoloadInterstitialHandler() {
        try {
            autoloadInterstitialHandler?.removeCallbacksAndMessages(null)
            hasWorkingAutoloadInterstitialHandler = false
        } catch (e: Exception) {
        }

    }

    fun stopAutoloadRewardedHandler() {
        try {
            autoloadRewardedHandler?.removeCallbacksAndMessages(null)
            hasWorkingAutoloadRewardedHandler = false
        } catch (e: Exception) {
        }
    }

    fun destroy(activity: Activity) {
        releaseAllFullScreenDisplayLeases()
        clearLoadingView()

        adPlatforms.forEach {
            it.platformInstance.destroy(activity)
        }

        stopAutoloadInterstitialHandler()
        stopAutoloadRewardedHandler()
        handlerThread?.quit()
        handlerThread = null
    }

    // TODO this fun will check. and it will remove if unnecessary
    fun onDestroyActivity(activity: Activity) {
        releaseFullScreenDisplayLeases(activity)
        removeLoadingViewFromActivity(activity)
        //stopAutoloadInterstitialHandler()
        //stopAutoloadRewardedHandler()
        //destroyBannersAndMrecs(activity)
    }

    // TODO this fun will check. and it will remove if unnecessary
    fun destroyBannersAndMrecs(activity: Activity) {

        placementGroups.indices.forEach { placementGroupIndex ->
            _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
                .forEach { platform ->
                    if (platform.platformInstance.isBannerLoaded(placementGroupIndex)) {
                        platform.platformInstance.destroyBanner(activity, placementGroupIndex)
                    }
                }

            _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)
                .forEach { platform ->
                    if (platform.platformInstance.isMrecLoaded(placementGroupIndex)) {
                        platform.platformInstance.destroyMrec(activity, placementGroupIndex)
                    }
                }
        }
    }


    fun onCreate(activity: Activity) {
        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.onCreate(activity)
        }
    }

    fun onResume(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.onResume(activity)
        }
    }

    fun onPause(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.onPause(activity)
        }
    }

    fun onStop(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.onStop(activity)
        }
    }

    fun randInt(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }


    @JvmOverloads
    fun loadSmallNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.NATIVE, placementGroupIndex, null, listener)
        validatePlacementGroup(placementGroupIndex)

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE, count, listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadMediumNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyLoadDisabled(AdFormatEnum.NATIVE_MEDIUM, placementGroupIndex, null, listener)
        validatePlacementGroup(placementGroupIndex)

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE_MEDIUM, count, listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return notifyLoadDisabled(nativeAdFormat, placementGroupIndex, null, listener)
        validatePlacementGroup(placementGroupIndex)

        _loadNativeFromAllNetworks(activity, nativeAdFormat, count, listener, placementGroupIndex)
    }

    private fun _loadNativeFromAllNetworks(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int) {
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(nativeAdFormat, placementGroupIndex)
        if (nativeAdPlatforms.size == 0) {
            listener?.onError(
                AdManagerError(
                    format = nativeAdFormat,
                    placementGroupIndex = placementGroupIndex,
                    message = "No ${nativeAdFormat.name.lowercase(Locale.ENGLISH)} ad platform is configured for placement group index $placementGroupIndex"
                )
            )
            return
        }

        val completedPlatforms = mutableSetOf<AdPlatformTypeEnum>()
        val successfulPlatforms = mutableSetOf<AdPlatformTypeEnum>()
        val platformErrors = mutableListOf<AdPlatformError>()

        nativeAdPlatforms.forEach { platform ->
            platform.platformInstance.loadNativeAds(activity, nativeAdFormat, count, object : AdPlatformLoadListener() {
                override fun onPlatformError(error: AdPlatformError) {
                    platformErrors.add(error)
                    completedPlatforms.add(error.platform)
                    listener?.onPlatformError(error)
                    if (completedPlatforms.size == nativeAdPlatforms.size && successfulPlatforms.isEmpty()) {
                        listener?.onError(
                            AdManagerError(
                                nativeAdFormat,
                                placementGroupIndex,
                                nativeAdPlatforms.map { it.platformInstance.platform },
                                platformErrors.toList(),
                                "No ${nativeAdFormat.name.lowercase(Locale.ENGLISH)} found in all platforms"
                            )
                        )
                    }
                }

                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                    successfulPlatforms.add(adPlatformEnum)
                    completedPlatforms.add(adPlatformEnum)
                    listener?.onLoaded(adPlatformEnum)
                }
            }, placementGroupIndex)
        }

    }

    private val lastShowedAdNetworkForNativeAds = mutableMapOf<String, AdPlatformModel>()

    fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0): Boolean {
        if (!showAds) {
            notifyShowDisabled(nativeAdFormat, placementGroupIndex, null, listener)
            return false
        }
        validatePlacementGroup(placementGroupIndex)

        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(nativeAdFormat, placementGroupIndex)
        if (nativeAdPlatforms.size == 0) {
            listener?.onError(
                AdManagerError(
                    format = nativeAdFormat,
                    placementGroupIndex = placementGroupIndex,
                    message = "No ${nativeAdFormat.name.lowercase(Locale.ENGLISH)} ad platform is configured for placement group index $placementGroupIndex"
                )
            )
            return false
        }


        // set current show ad network
        var currentShowAdNetwork: AdPlatformModel? = null
        try {
            val nativeAdPlatformsHasLoadedNativeAds: List<AdPlatformModel> = nativeAdPlatforms.filter {
                it.platformInstance.hasLoadedNative(nativeAdFormat)
            }
            if (nativeAdPlatformsHasLoadedNativeAds.size > 1) {
                if (lastShowedAdNetworkForNativeAds[nativeAdFormat.name] != null) {
                    val lastShowedIndex = nativeAdPlatformsHasLoadedNativeAds.indexOf(lastShowedAdNetworkForNativeAds[nativeAdFormat.name])

                    // eğer son öğeyse
                    if (lastShowedIndex == (nativeAdPlatformsHasLoadedNativeAds.size - 1)) {
                        currentShowAdNetwork = nativeAdPlatformsHasLoadedNativeAds[0]
                    } else {
                        currentShowAdNetwork = nativeAdPlatformsHasLoadedNativeAds[lastShowedIndex + 1]
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        var showed = false
        for (i in 0 until nativeAdPlatforms.size) {
            val platform = nativeAdPlatforms[i]
            val hasLoadedNative = platform.platformInstance.hasLoadedNative(nativeAdFormat)

            if (currentShowAdNetwork != null) {
                if (currentShowAdNetwork != platform) {
                    continue
                }
            }

            if (hasLoadedNative) {
                platform.platformInstance.showNative(activity, nativeAdFormat, containerView, listener, placementGroupIndex)
                lastShowedAdNetworkForNativeAds[nativeAdFormat.name] = platform
                showed = true
            }

            if (showed) break

        }

        if (!showed) {
            listener?.onError(
                AdManagerError(
                    nativeAdFormat,
                    placementGroupIndex,
                    nativeAdPlatforms.map { it.platformInstance.platform },
                    message = "No loaded native ad"
                )
            )
        }


        return showed
    }


    fun hasLoadedNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int = 0): Boolean {
        if (!showAds) return false
        validatePlacementGroup(placementGroupIndex)

        return getLoadedNativeAdsCount(activity, nativeAdFormat) > 0
    }

    fun getLoadedNativeAdsCount(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int = 0): Int {
        if (!showAds) return 0
        validatePlacementGroup(placementGroupIndex)

        var count = 0
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(nativeAdFormat, placementGroupIndex)
        if (nativeAdPlatforms.size == 0) {
            return count
        }

        nativeAdPlatforms.forEach { platform ->
            count += platform.platformInstance.getNativeAds(activity, nativeAdFormat).size
        }

        return count

    }

    fun applovinDebugger(activity: Activity) {
        AppLovinSdk.getInstance(activity).showMediationDebugger()
    }

    fun applovinCreativeDebugger(activity: Activity) {
        AppLovinSdk.getInstance(activity).showCreativeDebugger()

    }
}
