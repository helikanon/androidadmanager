package com.helikanonlib.admanager


import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
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
    var randomInterval: Int = 40
    var interstitialMinElapsedSecondsToNextShow: Int = 40
    var rewardedMinElapsedSecondsToNextShow: Int = 40
    var isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = false

    var adPlatforms: MutableList<AdPlatformModel> = mutableListOf<AdPlatformModel>()

    var globalInterstitialShowListener: AdPlatformShowListener? = null
    var globalRewardedShowListener: AdPlatformShowListener? = null
    var globalInterstitialLoadListener: AdPlatformLoadListener? = null
    var globalRewardedLoadListener: AdPlatformLoadListener? = null

    var adPlatformSortByAdFormat: MutableMap<String, List<AdPlatformTypeEnum>> = mutableMapOf()
    var placementGroups = ArrayList<String>()

    // handlers
    private var isHandlerAvailableForLoads = false
    private var handlerThread: HandlerThread? = null
    private var autoloadInterstitialHandler: Handler? = null
    private var autoloadRewardedHandler: Handler? = null
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    var lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

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

    fun initializePlatformsWithActivity(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.initialize(activity, testMode)

            if (testMode) {
                platform.platformInstance.enableTestMode(activity.applicationContext, deviceId)
            }
        }
    }

    fun initializePlatforms(context: Context) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.initialize(context, testMode)

            if (testMode) {
                platform.platformInstance.enableTestMode(context, deviceId)
            }
        }
    }

    fun start(activity: Activity) {
        if (autoLoadForInterstitial) {
            placementGroups.forEachIndexed { index, pgName ->
                if (index > 0) return@forEachIndexed
                loadInterstitial(activity, listener = null, platform = null, parallel = false, placementGroupIndex = getPlacementGroupIndexByName(pgName))
            }

        }

        if (autoLoadForRewarded) {
            placementGroups.forEachIndexed { index, pgName ->
                if (index > 0) return@forEachIndexed
                loadRewarded(activity, null, null, false, getPlacementGroupIndexByName(pgName))
            }
        }
    }

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
    var loadingView: AdsLoadingCustomView? = null
    fun initLoadingView(activity: Activity, rootView: ViewGroup? = null) {
        if (loadingView != null) {
            // loadingView?.findViewById<TextView>(R.id.textViewCloseAdsLoading)?.visibility = View.VISIBLE
            // loadingView?.rootView?.visibility = View.VISIBLE

            rootView?.let {
                setLoadingLayoutParams(rootView)
            }

            if (loadingView?.parent != null) {
                (loadingView?.parent as ViewGroup).removeView(loadingView)
            }
            return
        }
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
            e.printStackTrace()
        }
    }

    fun removeLoadingViewFromActivity(activity: Activity) {
        try {
            if (loadingView == null) return
            val activityRootView = activity.findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0) as ViewGroup

            var className = activityRootView::class.simpleName

            if (className == "NestedScrollView") {
                (activityRootView.getChildAt(0) as ViewGroup).removeView(loadingView)
            } else {
                activityRootView.removeView(loadingView)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addAdPlatform(adPlatform: AdPlatformModel) = apply { this.adPlatforms.add(adPlatform) }
    fun getAdPlatformByType(platformType: AdPlatformTypeEnum): AdPlatformModel? {
        val filteredPlatforms = adPlatforms.filter { it -> it.platformInstance.platform == platformType }
        return if (filteredPlatforms.size > 0) filteredPlatforms[0] else null
    }


    fun getPlacementGroupIndexByName(pgName: String): Int {
        return placementGroups.indexOf(pgName)
    }

    fun getPlacementGroupNameByIndex(pgIndex: Int): String {
        return placementGroups[pgIndex]
    }

    /**
     * example :
     * adManager.setAdPlatformSortByAdFormatStr("interstitial", "ironsource,mopub,admob,facebook")
     * adManager.setAdPlatformSortByAdFormatStr("banner", "ironsource,facebook,admob,mopub")
     */
    fun setAdPlatformSortByAdFormatStr(placementGroupIndex: Int, adFormatName: String, adPlatformsStr: String) {
        try {
            val _afPlatformsArr = adPlatformsStr.splitIgnoreEmpty(",").map {
                if (!it.isNullOrEmpty()) {
                    AdPlatformTypeEnum.valueOf(it.trim().uppercase(Locale.ENGLISH))
                } else {
                    AdPlatformTypeEnum.valueOf(it.trim().uppercase(Locale.ENGLISH))
                }
            }
            adPlatformSortByAdFormat.put(
                adFormatName.uppercase(Locale.ENGLISH) + "__" + getPlacementGroupNameByIndex(placementGroupIndex), _afPlatformsArr
            )
        } catch (e: Exception) {
            Log.e("AdManager", "setAdPlatformSortByAdFormatStr >> ${e.message}")
        }
    }


    private fun _getAdPlatformsWithSortedByAdFormat(adFormatEnum: AdFormatEnum, placementGroupIndex: Int): MutableList<AdPlatformModel> {
        val filteredAdPlatforms = mutableListOf<AdPlatformModel>()
        val adFormatSort = adPlatformSortByAdFormat[adFormatEnum.name.uppercase(Locale.ENGLISH) + "__" + getPlacementGroupNameByIndex(placementGroupIndex)]

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
        if (!showAds) return

        if (platform == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
                interstitialAdPlatforms.forEach forEach@{ _platform ->
                    if (!_platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                        _loadInterstitial(activity, listener, platform = _platform, placementGroupIndex)
                    }
                }
            } else {
                _loadInterstitialFromFirstAvailable(activity, listener, 0, placementGroupIndex)
            }
        } else {
            _loadInterstitial(activity, listener = listener, platform = platform, placementGroupIndex)
        }
    }

    private fun _loadInterstitialFromFirstAvailable(activity: Activity, listener: AdPlatformLoadListener? = null, index: Int = 0, placementGroupIndex: Int) {
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
        if (index >= interstitialAdPlatforms.size) {
            return
        }
        val platform = interstitialAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((index + 1) < interstitialAdPlatforms.size) {
                    activity.runOnUiThread { _loadInterstitialFromFirstAvailable(activity, listener, index + 1, placementGroupIndex) }
                } else {
                    globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms", adPlatformEnum)
                    listener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms", adPlatformEnum)
                }
                globalInterstitialLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }
        }
        platform.platformInstance.loadInterstitial(activity, _listener, placementGroupIndex)
    }

    private fun _loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

                globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {

                globalInterstitialLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
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

        activity.runOnUiThread {
            addLoadingViewToActivity(activity)
        }
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, false)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                // it will come here for each ad platforms, so we wants only call _showInterstitial
                // after try all platforms
                // _showInterstitial will trigger user listener
                if (errorMode == AdErrorMode.MANAGER) {
                    _showInterstitial(activity, shownWhere, listener, platform, placementGroupIndex, false)
                }


            }
        }
        loadInterstitial(activity, loadListener, platform, false, placementGroupIndex)
    }

    @JvmOverloads
    fun showInterstitialForTimeStrategy(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

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
        }
    }

    @JvmOverloads
    fun showInterstitial(
        activity: Activity, shownWhere: String = DEFAULT_INTERSTITIAL_SHOWN_WHERE_NAME, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

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
        if (!showAds) return true

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                globalInterstitialShowListener?.onClosed(adPlatformEnum)
                listener?.onClosed(adPlatformEnum)
                saveLastShowDate(AdFormatEnum.INTERSTITIAL)

                // on close load new one for next show
                if (autoLoadForInterstitial) {
                    _autoloadInterstitialByHandler(activity, null, null)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                stopAutoloadInterstitialHandler()

                globalInterstitialShowListener?.onDisplayed(adPlatformEnum)
                listener?.onDisplayed(adPlatformEnum)

                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialShowListener?.onClicked(adPlatformEnum)
                listener?.onClicked(adPlatformEnum)
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalInterstitialShowListener?.onRewarded(type, amount, adPlatformEnum)
                listener?.onRewarded(type, amount, adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                activity.runOnUiThread {
                    removeLoadingViewFromActivity(activity)
                }

                if (autoLoadForInterstitial) {
                    if (isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode && loadAndShowIfNotExistsAdsOnAutoloadMode) {
                        stopAutoloadInterstitialHandler()
                        loadAndShowInterstitial(activity, shownWhere, listener, platform, placementGroupIndex)
                    } else {
                        _autoloadInterstitialByHandler(activity, null, null)

                        globalInterstitialShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                        // listener?.onError(errorMode, errorMessage, adPlatformEnum) // call for adplatform
                        listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // call for manager

                    }
                } else {
                    globalInterstitialShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                    // listener?.onError(errorMode, errorMessage, adPlatformEnum) // call for adplatform
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // call for manager
                }

            }
        }

        var hasLoadedInterstitial = false
        if (platform != null) {
            if (platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                platform.platformInstance.showInterstitial(activity, shownWhere, _listener, placementGroupIndex)
                hasLoadedInterstitial = true
            }
        } else {
            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ platform ->
                    if (platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                        platform.platformInstance.showInterstitial(activity, shownWhere, _listener, placementGroupIndex)
                        hasLoadedInterstitial = true
                        return@breaker
                        //return@forEach
                    }
                }
            }
        }

        if (!hasLoadedInterstitial) {
            // globalInterstitialShowListener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded", null)
            _listener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded", null)

        }

        return hasLoadedInterstitial
    }

    var lastPostDelayedSetTimeForInterstitialLoad: Date? = null
    private fun _autoloadInterstitialByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {

        if (!isHandlerAvailableForLoads) {
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index < 1) {
                            loadInterstitial(activity, listener, platform, false, getPlacementGroupIndexByName(pgName))
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return
        }

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


    var lastPostDelayedSetTimeForRewardedLoad: Date? = null
    private fun _autoloadRewardedByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {

        if (!isHandlerAvailableForLoads) {
            try {
                activity.runOnUiThread {
                    placementGroups.forEachIndexed { index, pgName ->
                        if (index > 0) return@forEachIndexed
                        loadRewarded(activity, listener, platform, false, getPlacementGroupIndexByName(pgName))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
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
        if (!showAds) return


        if (platform == null) {
            var startFrom = 0

            // if already banner loaded, start from this platform
            val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
            if (bannerAdPlatforms.size > 0) {
                run breaker@{
                    bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->

                        if (_platform.platformInstance.isBannerLoaded(placementGroupIndex)) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }
            _showBannerFromFirstAvailable(activity, containerView, listener, startFrom, placementGroupIndex)
        } else {
            _showBanner(activity, containerView, listener, platform, placementGroupIndex)
        }
    }

    private fun _showBannerFromFirstAvailable(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platformIndex: Int = 0, placementGroupIndex: Int) {
        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
        if (platformIndex >= bannerAdPlatforms.size) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((platformIndex + 1) < bannerAdPlatforms.size) {
                    activity.runOnUiThread { _showBannerFromFirstAvailable(activity, containerView, listener, platformIndex + 1, placementGroupIndex) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // there is no banner ads. Tried on all platforms
                }
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }
        }

        val platform = bannerAdPlatforms[platformIndex]
        platform.platformInstance.showBanner(activity, containerView, _listener, placementGroupIndex)
    }

    private fun _showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {

        platform.platformInstance.showBanner(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }
        }, placementGroupIndex)
    }

    fun hasLoadedInterstitial(platform: AdPlatformModel? = null, placementGroupIndex: Int): Boolean {
        var hasLoaded = false

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL, placementGroupIndex)
        run breaker@{

            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ _platform ->
                    if (platform != null && _platform.platformInstance.platform != platform.platformInstance.platform) {
                        return@breaker
                    }

                    if (_platform.platformInstance.isInterstitialLoaded(placementGroupIndex)) {
                        hasLoaded = true
                        return@breaker
                    }
                }
            }


        }

        return hasLoaded
    }

    fun hasLoadedRewarded(platform: AdPlatformModel? = null, placementGroupIndex: Int = 0): Boolean {
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
        if (!showAds) return

        if (platform == null) {
            if (parallel) {
                // send true parallel when first load after init ads. Else you dont need parallel load
                val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
                rewardedAdPlatforms.forEach forEach@{ _platform ->
                    if (!_platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                        _loadRewarded(activity, listener, platform = _platform, placementGroupIndex)
                    }
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
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

                globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)
            }

        }
        platform.platformInstance.loadRewarded(activity, _listener, placementGroupIndex)
    }

    private fun _loadRewardedFromFirstAvailable(activity: Activity, listener: AdPlatformLoadListener? = null, index: Int = 0, placementGroupIndex: Int) {

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
        if (index >= rewardedAdPlatforms.size) {
            return
        }
        val platform = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((index + 1) < rewardedAdPlatforms.size) {
                    activity.runOnUiThread { _loadRewardedFromFirstAvailable(activity, listener, index + 1, placementGroupIndex) }
                } else {
                    globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms", null)
                    listener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms", adPlatformEnum)

                }
                globalRewardedLoadListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(errorMode, errorMessage, adPlatformEnum)

            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedLoadListener?.onLoaded(adPlatformEnum)
                listener?.onLoaded(adPlatformEnum)

            }
        }

        platform.platformInstance.loadRewarded(activity, _listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadAndShowRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                _showRewarded(activity, listener, platform, placementGroupIndex)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    _showRewarded(activity, listener, platform, placementGroupIndex)
                }
            }
        }
        loadRewarded(activity, loadListener, platform, false, placementGroupIndex)
    }

    @JvmOverloads
    fun showRewarded(
        activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null,
        placementGroupIndex: Int = 0, loadAndShowIfNotExistsAdsOnAutoloadMode: Boolean = true
    ) {
        if (!showAds) return

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
        if (!showAds) return true

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED, placementGroupIndex)
        val _listener = object : AdPlatformShowListener() {
            override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
                // on close load new one for next show
                globalRewardedShowListener?.onClosed(adPlatformEnum)
                listener?.onClosed(adPlatformEnum)
                saveLastShowDate(AdFormatEnum.REWARDED)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, null, platform)
                }
            }

            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                stopAutoloadRewardedHandler()

                globalRewardedShowListener?.onDisplayed(adPlatformEnum)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedShowListener?.onClicked(adPlatformEnum)
                listener?.onClicked(adPlatformEnum)
            }

            override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                globalRewardedShowListener?.onRewarded(type, amount, adPlatformEnum)
                listener?.onRewarded(type, amount, adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {

                globalRewardedShowListener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
                // listener?.onError(errorMode, errorMessage, adPlatformEnum)

                if (autoLoadForRewarded) {
                    _autoloadRewardedByHandler(activity, null, platform)
                }
            }
        }

        var hasLoadedRewarded = false

        if (platform != null) {
            if (platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                platform.platformInstance.showRewarded(activity, _listener, placementGroupIndex)
                hasLoadedRewarded = true
            }
        } else {
            run breaker@{
                rewardedAdPlatforms.forEach forEach@{ platform ->
                    if (platform.platformInstance.isRewardedLoaded(placementGroupIndex)) {
                        platform.platformInstance.showRewarded(activity, _listener, placementGroupIndex)
                        hasLoadedRewarded = true
                        return@breaker
                    }
                }
            }

        }

        if (!hasLoadedRewarded) {
            _listener?.onError(AdErrorMode.MANAGER, "There is no loaded rewarded. Tried in all platforms", null)

            /*globalRewardedShowListener?.onError(AdErrorMode.MANAGER, "There is no loaded rewarded. Tried in all platforms", null)
            if (autoLoadForRewarded) {
                _autoloadRewardedByHandler(activity, null, platform)
            }*/
        }

        return hasLoadedRewarded
    }

    @JvmOverloads
    fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)

        if (mrecAdPlatforms.size == 0) {
            listener?.onError(AdErrorMode.MANAGER, "no networks for medium banner", null)
        }

        if (platform == null) {
            var startFrom = 0

            // if already mrec banner loaded, start from this platform
            if (mrecAdPlatforms.size > 0) {
                run breaker@{
                    mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.platformInstance.isMrecLoaded(placementGroupIndex)) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }

            _showMrecFromFirstAvailable(activity, containerView, listener, startFrom, placementGroupIndex)
        } else {
            _showMrec(activity, containerView, listener, platform, placementGroupIndex)
        }
    }

    private fun _showMrecFromFirstAvailable(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0, placementGroupIndex: Int) {
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)
        if (index >= mrecAdPlatforms.size) {
            return
        }
        val platform = mrecAdPlatforms[index]

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                if ((index + 1) < mrecAdPlatforms.size) {
                    activity.runOnUiThread { _showMrecFromFirstAvailable(activity, containerView, listener, index + 1, placementGroupIndex) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum) // not found any ads in all platforms
                }
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }
        }, placementGroupIndex)
    }

    private fun _showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel, placementGroupIndex: Int) {

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
                listener?.onError(AdErrorMode.MANAGER, errorMessage, adPlatformEnum)
            }
        }, placementGroupIndex)
    }

    fun saveLastShowDate(adFormatEnum: AdFormatEnum) {
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
        //stopAutoloadInterstitialHandler()
        //stopAutoloadRewardedHandler()
        //destroyBannersAndMrecs(activity)
    }

    // TODO this fun will check. and it will remove if unnecessary
    fun destroyBannersAndMrecs(activity: Activity) {

        placementGroups.forEachIndexed { placementGroupIndex, pgName ->
            val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER, placementGroupIndex)
            if (bannerAdPlatforms.size > 0) {
                run breaker@{
                    bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.platformInstance.isBannerLoaded()) {
                            _platform.platformInstance.destroyBanner(activity)
                            return@breaker
                        }
                    }
                }
            }

            val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC, placementGroupIndex)
            if (mrecAdPlatforms.size > 0) {
                run breaker@{
                    mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.platformInstance.isMrecLoaded()) {

                            _platform.platformInstance.destroyMrec(activity)
                            return@breaker
                        }
                    }
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
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE, count, listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadMediumNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, AdFormatEnum.NATIVE_MEDIUM, count, listener, placementGroupIndex)
    }

    @JvmOverloads
    fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0) {
        if (!showAds) return

        _loadNativeFromAllNetworks(activity, nativeAdFormat, count, listener, placementGroupIndex)
    }

    private fun _loadNativeFromAllNetworks(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int) {
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(nativeAdFormat, placementGroupIndex)
        if (nativeAdPlatforms.size == 0) {
            return
        }

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onError(errorMode, errorMessage, adPlatformEnum)
            }

            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                listener?.onLoaded(adPlatformEnum)
            }
        }

        nativeAdPlatforms.forEach { platform ->
            platform.platformInstance.loadNativeAds(activity, nativeAdFormat, count, _listener, placementGroupIndex)
        }

    }

    var lastShowedAdNetworkForNativeAds: MutableMap<String, AdPlatformModel> = mutableMapOf()

    fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0): Boolean {
        val nativeAdPlatforms = _getAdPlatformsWithSortedByAdFormat(nativeAdFormat, placementGroupIndex)
        if (nativeAdPlatforms.size == 0) {
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
            listener?.onError(AdErrorMode.MANAGER, "no loaded native ad", null)
        }


        return showed
    }


    fun hasLoadedNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int = 0): Boolean {
        return getLoadedNativeAdsCount(activity, nativeAdFormat) > 0
    }

    fun getLoadedNativeAdsCount(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int = 0): Int {
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
}
