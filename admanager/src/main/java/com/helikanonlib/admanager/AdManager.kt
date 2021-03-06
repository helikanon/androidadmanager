package com.helikanonlib.admanager


import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.widget.RelativeLayout
import java.util.*

// TODO interstitial,banner,rewarded,mrec show function will take placementId parameter
// TODO ADD AD LOADING WARNING LAYOUT >> var a = (activity.findViewById<ViewGroup>(R.id.content)).getChildAt(0) as ViewGroup
// TODO FUTURE = add native ads support
// TODO add option = dont show until date for interstitial
class AdManager {
    var testMode: Boolean = false
    var deviceId: String = ""
    var showAds: Boolean = true
    var autoLoad: Boolean = true
    var autoLoadDelay: Long = 10
    var randomInterval: Int = 40
    var interstitialMinElapsedSecondsToNextShow: Int = 40
    var rewardedMinElapsedSecondsToNextShow: Int = 40

    var adPlatforms: MutableList<AdPlatformModel> = mutableListOf<AdPlatformModel>()

    var globalInterstitialShowListener: AdPlatformShowListener? = null
    var globalRewardedShowListener: AdPlatformShowListener? = null
    var globalInterstitialLoadListener: AdPlatformLoadListener? = null
    var globalRewardedLoadListener: AdPlatformLoadListener? = null

    var adPlatformSortByAdFormat: MutableMap<AdFormatEnum, List<AdPlatformTypeEnum>> = mutableMapOf()

    // handlers
    var handlerThread = HandlerThread("admanager-bg-thread");
    lateinit var autoloadInterstitialHandler: Handler
    lateinit var autoloadRewardedHandler: Handler
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    var lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

    init {
        handlerThread.start()
        autoloadInterstitialHandler = Handler(handlerThread.looper)
        autoloadRewardedHandler = Handler(handlerThread.looper)
    }


    constructor()

    private constructor(builder: AdManager.Builder) : this() {

        this.testMode = builder.testMode
        this.deviceId = builder.deviceId
        this.showAds = builder.showAds
        this.autoLoad = builder.autoLoad
        this.autoLoadDelay = builder.autoLoadDelay
        this.randomInterval = builder.randomInterval
        this.interstitialMinElapsedSecondsToNextShow = builder.interstitialMinSeconds
        this.rewardedMinElapsedSecondsToNextShow = builder.rewardedMinSeconds
        this.adPlatforms = builder.adPlatforms
    }

    fun initialize(activity: Activity) {
        if (!showAds) return

        initializePlatforms(activity)
        start(activity)
    }

    fun initializePlatforms(activity: Activity) {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.platformInstance.initialize(activity)

            if (testMode) {
                platform.platformInstance.enableTestMode(deviceId)
            }
        }
    }

    fun start(activity: Activity) {
        if (autoLoad) {
            loadInterstitial(activity)
            loadRewarded(activity)
        }
    }

    fun enableTestMode(deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        adPlatforms.forEach forEach@{ platform ->
            if (testMode) {
                platform.platformInstance.enableTestMode(deviceId)
            }
        }
    }

    fun addAdPlatform(adPlatform: AdPlatformModel) = apply { this.adPlatforms.add(adPlatform) }
    fun getAdPlatformByType(platformType: AdPlatformTypeEnum): AdPlatformModel? {
        val filteredPlatforms = adPlatforms.filter { it -> it.platformInstance.platform == platformType }
        return if (filteredPlatforms.size > 0) filteredPlatforms[0] else null
    }

    /**
     * example :
     * adManager.setAdPlatformSortByAdFormatStr("interstitial", "ironsource,mopub,admob,facebook")
     * adManager.setAdPlatformSortByAdFormatStr("banner", "ironsource,facebook,admob,startapp,mopub")
     */
    fun setAdPlatformSortByAdFormatStr(adFormatName: String, adPlatformsStr: String) {
        val _afPlatformsArr = adPlatformsStr.split(",").map {
            AdPlatformTypeEnum.valueOf(it.trim().toUpperCase(Locale.ENGLISH))
        }
        adPlatformSortByAdFormat.put(
            AdFormatEnum.valueOf(
                adFormatName.trim().toUpperCase(Locale.ENGLISH)
            ), _afPlatformsArr
        )
    }


    private fun _getAdPlatformsWithSortedByAdFormat(adFormatEnum: AdFormatEnum): MutableList<AdPlatformModel> {
        val filteredAdPlatforms = mutableListOf<AdPlatformModel>()
        val adFormatSort = adPlatformSortByAdFormat[adFormatEnum]

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
                    else -> false
                }

                if (isAdd) {
                    filteredAdPlatforms.add(adPlatform)
                }
            }
        }

        return filteredAdPlatforms
    }

    fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            _loadInterstitialFromFirstAvailable(activity, listener, 0)
        } else {
            _loadInterstitial(activity, listener = listener, platform = platform)
        }
    }

    private fun _loadInterstitialFromFirstAvailable(activity: Activity, listener: AdPlatformLoadListener? = null, index: Int = 0) {
        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL)
        if (interstitialAdPlatforms.size == 0) {
            return
        }
        val platform = interstitialAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < interstitialAdPlatforms.size) {
                    activity.runOnUiThread { _loadInterstitialFromFirstAvailable(activity, listener, index + 1) }
                } else {
                    globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms")
                    listener?.onError(AdErrorMode.MANAGER, "No interstitial found in all platforms")
                }
                globalInterstitialLoadListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage)
            }

            override fun onLoaded() {
                globalInterstitialLoadListener?.onLoaded()
                listener?.onLoaded()
            }
        }
        platform.platformInstance.loadInterstitial(activity, _listener)
    }

    private fun _loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                globalInterstitialLoadListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage)

                globalInterstitialLoadListener?.onError(AdErrorMode.MANAGER, errorMessage)
                listener?.onError(AdErrorMode.MANAGER, errorMessage)
            }

            override fun onLoaded() {

                globalInterstitialLoadListener?.onLoaded()
                listener?.onLoaded()
            }

        }
        platform.platformInstance.loadInterstitial(activity, _listener)
    }

    @JvmOverloads
    fun loadAndShowInterstitial(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {

        /*
        call _showInterstitial(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showInterstitial
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded() {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(activity, listener, platform)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                // it will come here for each ad platforms, so we wants only call _showInterstitial
                // after try all platforms
                // _showInterstitial will trigger user listener
                if (errorMode == AdErrorMode.MANAGER) {
                    _showInterstitial(activity, listener, platform)
                }

            }
        }
        loadInterstitial(activity, loadListener, platform)
    }

    @JvmOverloads
    fun showInterstitialForTimeStrategy(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        var isAvailableToShow = true
        var lastShowDate = lastShowDateByAdFormat.get(AdFormatEnum.INTERSTITIAL)

        if (lastShowDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastShowDate.time) / 1000
            val requiredElapsedTime =
                randInt(0, randomInterval) + interstitialMinElapsedSecondsToNextShow
            isAvailableToShow = elapsedSeconds > requiredElapsedTime
        }

        if (isAvailableToShow) {
            showInterstitial(activity, listener, platform)
        }
    }

    @JvmOverloads
    fun showInterstitial(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (autoLoad) {
            val isShowed = _showInterstitial(activity, listener, platform)
            if (!isShowed) {
                stopAutoloadInterstitialHandler()
                loadAndShowInterstitial(activity, listener, platform)
            }
        } else {
            loadAndShowInterstitial(activity, listener, platform)
        }
    }

    private fun _showInterstitial(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null): Boolean {
        if (!showAds) return true

        val interstitialAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.INTERSTITIAL)

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed() {
                // on close load new one for next show
                if (autoLoad) {
                    _autoloadInterstitialByHandler(activity, null, platform)
                }
                globalInterstitialShowListener?.onClosed()
                listener?.onClosed()
            }

            override fun onDisplayed() {
                globalInterstitialShowListener?.onDisplayed()
                listener?.onDisplayed()
            }

            override fun onClicked() {
                globalInterstitialShowListener?.onClicked()
                listener?.onClicked()
            }

            override fun onRewarded(type: String?, amount: Int?) {
                globalInterstitialShowListener?.onRewarded()
                listener?.onRewarded()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                globalInterstitialShowListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage) // call for adplatform
                listener?.onError(AdErrorMode.MANAGER, errorMessage) // call for manager
            }
        }

        var isShowed = false
        if (platform != null) {
            if (platform.platformInstance.isInterstitialLoaded()) {
                platform.platformInstance.showInterstitial(activity, _listener)
                isShowed = true
            }
        } else {
            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ platform ->
                    if (!platform.platformInstance.isInterstitialLoaded()) {
                        return@forEach
                    }

                    platform.platformInstance.showInterstitial(activity, _listener)
                    isShowed = true
                    return@breaker
                }
            }
        }

        if (isShowed) {
            saveLastShowDate(AdFormatEnum.INTERSTITIAL)
        } else {
            globalInterstitialShowListener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded")
            listener?.onError(AdErrorMode.MANAGER, "there is no loaded interstitial for show. All platforms is not loaded")
            if (autoLoad) {
                _autoloadInterstitialByHandler(activity, null, platform)
            }
        }

        return isShowed
    }

    private fun _autoloadInterstitialByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        // load new if there is no ads
        if (hasWorkingAutoloadInterstitialHandler) return

        stopAutoloadInterstitialHandler()
        hasWorkingAutoloadInterstitialHandler = true
        autoloadInterstitialHandler.postDelayed({
            try {
                activity.runOnUiThread {
                    loadInterstitial(activity, listener, platform)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadInterstitialHandler = false
        }, autoLoadDelay * 1000)
    }

    @JvmOverloads
    fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return


        if (platform == null) {
            var startFrom = 0

            // if already banner loaded, start from this platform
            val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER)
            if (bannerAdPlatforms.size > 0) {
                run breaker@{
                    bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.platformInstance.isBannerLoaded()) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }
            _showBannerFromFirstAvailable(activity, containerView, listener, startFrom)
        } else {
            _showBanner(activity, containerView, listener, platform)
        }
    }

    private fun _showBannerFromFirstAvailable(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0) {
        var bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER)
        if (bannerAdPlatforms.size == 0) {
            return
        }

        val _listener = object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < bannerAdPlatforms.size) {
                    activity.runOnUiThread { _showBannerFromFirstAvailable(activity, containerView, listener, index + 1) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage) // there is no banner ads. Tried on all platforms
                }
                listener?.onError(errorMode, errorMessage)
            }
        }

        val platform = bannerAdPlatforms[index]
        platform.platformInstance.showBanner(activity, containerView, _listener)
    }

    private fun _showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.platformInstance.showBanner(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                listener?.onError(errorMode, errorMessage)
                listener?.onError(AdErrorMode.MANAGER, errorMessage)
            }
        })
    }

    fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            _loadRewardedFromFirstAvailable(activity, listener)
        } else {
            _loadRewarded(activity, listener, platform)
        }
    }

    private fun _loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                globalRewardedLoadListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage)

                globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, errorMessage)
                listener?.onError(AdErrorMode.MANAGER, errorMessage)
            }

            override fun onLoaded() {
                globalRewardedLoadListener?.onLoaded()
                listener?.onLoaded()
            }

        }
        platform.platformInstance.loadRewarded(activity, _listener)
    }

    private fun _loadRewardedFromFirstAvailable(activity: Activity, listener: AdPlatformLoadListener? = null, index: Int = 0) {

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED)
        if (rewardedAdPlatforms.size == 0) {
            return
        }
        val platform = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < rewardedAdPlatforms.size) {
                    activity.runOnUiThread { _loadRewardedFromFirstAvailable(activity, listener, index + 1) }
                } else {
                    globalRewardedLoadListener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms")
                    listener?.onError(AdErrorMode.MANAGER, "No rewarded found in all platforms")

                }
                globalRewardedLoadListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage)

            }

            override fun onLoaded() {
                globalRewardedLoadListener?.onLoaded()
                listener?.onLoaded()

            }
        }

        platform.platformInstance.loadRewarded(activity, _listener)
    }

    @JvmOverloads
    fun loadAndShowRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded() {
                _showRewarded(activity, listener, platform)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    _showRewarded(activity, listener, platform)
                }
            }
        }
        loadRewarded(activity, loadListener, platform)
    }

    @JvmOverloads
    fun showRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (autoLoad) {
            val isShowed = _showRewarded(activity, listener, platform)
            if (!isShowed) {
                stopAutoloadRewardedHandler()
                loadAndShowRewarded(activity, listener, platform)
            }
        } else {
            loadAndShowRewarded(activity, listener, platform)
        }
    }

    fun _showRewarded(activity: Activity, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null): Boolean {
        if (!showAds) return true

        val rewardedAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.REWARDED)
        val _listener = object : AdPlatformShowListener() {
            override fun onClosed() {
                // on close load new one for next show
                if (autoLoad) {
                    _autoloadRewardedByHandler(activity, null, platform)
                }
                globalRewardedShowListener?.onClosed()
                listener?.onClosed()
            }

            override fun onDisplayed() {
                globalRewardedShowListener?.onDisplayed()
                listener?.onDisplayed()
            }

            override fun onClicked() {
                globalRewardedShowListener?.onClicked()
                listener?.onClicked()
            }

            override fun onRewarded(type: String?, amount: Int?) {
                globalRewardedShowListener?.onRewarded()
                listener?.onRewarded()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                globalRewardedShowListener?.onError(errorMode, errorMessage)
                listener?.onError(errorMode, errorMessage)
                listener?.onError(AdErrorMode.MANAGER, errorMessage)
            }
        }

        var isShowed = false

        if (platform != null) {
            if (platform.platformInstance.isRewardedLoaded()) {
                platform.platformInstance.showRewarded(activity, _listener)
                isShowed = true
            }
        } else {
            run breaker@{
                rewardedAdPlatforms.forEach forEach@{ platform ->
                    if (platform.platformInstance.isRewardedLoaded()) {
                        platform.platformInstance.showRewarded(activity, _listener)
                        isShowed = true
                        return@breaker
                    }
                }
            }

        }

        if (isShowed) {
            saveLastShowDate(AdFormatEnum.REWARDED)
        } else {
            globalRewardedShowListener?.onError(AdErrorMode.MANAGER, "There is no loaded rewarded. Tried in all platforms")
            listener?.onError(AdErrorMode.MANAGER, "There is no loaded rewarded. Tried in all platforms")
            if (autoLoad) {
                _autoloadRewardedByHandler(activity, null, platform)
            }
        }

        return isShowed
    }

    @JvmOverloads
    fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            var startFrom = 0

            // if already mrec banner loaded, start from this platform
            val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC)
            if (mrecAdPlatforms.size > 0) {
                run breaker@{
                    mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.platformInstance.isMrecLoaded()) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }

            _showMrecFromFirstAvailable(activity, containerView, listener, startFrom)
        } else {
            _showMrec(activity, containerView, listener, platform)
        }
    }

    private fun _showMrecFromFirstAvailable(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0) {
        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC)
        if (mrecAdPlatforms.size == 0) {
            return
        }
        val platform = mrecAdPlatforms[index]

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < mrecAdPlatforms.size) {
                    activity.runOnUiThread { _showMrecFromFirstAvailable(activity, containerView, listener, index + 1) }
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage) // not found any ads in all platforms
                }
                listener?.onError(errorMode, errorMessage)
            }
        })
    }

    private fun _showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.platformInstance.showMrec(activity, containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                listener?.onError(errorMode, errorMessage)
                listener?.onError(AdErrorMode.MANAGER, errorMessage)
            }
        })
    }

    private fun _autoloadRewardedByHandler(activity: Activity, listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        // load new if there is no ads
        if (hasWorkingAutoloadRewardedHandler) return

        stopAutoloadRewardedHandler()
        hasWorkingAutoloadRewardedHandler = true
        autoloadRewardedHandler.postDelayed({
            try {
                activity.runOnUiThread {
                    loadRewarded(activity, listener, platform)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadRewardedHandler = false
        }, autoLoadDelay * 1000)
    }

    fun saveLastShowDate(adFormatEnum: AdFormatEnum) {
        lastShowDateByAdFormat.put(adFormatEnum, Date())
    }

    fun stopAutoloadInterstitialHandler() {
        autoloadInterstitialHandler.removeCallbacksAndMessages(null)
        hasWorkingAutoloadInterstitialHandler = false
    }

    fun stopAutoloadRewardedHandler() {
        autoloadRewardedHandler.removeCallbacksAndMessages(null)
        hasWorkingAutoloadRewardedHandler = false
    }

    fun destroy(activity: Activity) {
        adPlatforms.forEach {
            it.platformInstance.destroy(activity)
        }

        stopAutoloadInterstitialHandler()
        stopAutoloadRewardedHandler()
        handlerThread.quitSafely()
    }

    // TODO this fun will check. and it will remove if unnecessary
    fun onDestroyActivity(activity: Activity) {
        //stopAutoloadInterstitialHandler()
        //stopAutoloadRewardedHandler()
        //destroyBannersAndMrecs(activity)
    }

    // TODO this fun will check. and it will remove if unnecessary
    fun destroyBannersAndMrecs(activity: Activity) {
        val bannerAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.BANNER)
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

        val mrecAdPlatforms = _getAdPlatformsWithSortedByAdFormat(AdFormatEnum.MREC)
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

    data class Builder @JvmOverloads constructor(
        var testMode: Boolean = false,
        var deviceId: String = "",
        var showAds: Boolean = true,
        var autoLoad: Boolean = true,
        var autoLoadDelay: Long = 10,
        var randomInterval: Int = 40,
        var interstitialMinSeconds: Int = 40,
        var rewardedMinSeconds: Int = 40,
        val adPlatforms: MutableList<AdPlatformModel> = mutableListOf<AdPlatformModel>()
        //var adPlatforms: String = "facebook[interstitial,banner,rewarded],admob[interstitial,banner,rewarded],startapp[interstitial,banner,rewarded]"

    ) {
        fun testMode(enabled: Boolean) = apply { this.testMode = enabled }
        fun deviceId(deviceId: String) = apply { this.deviceId = deviceId }
        fun showAds(showAds: Boolean) = apply { this.showAds = showAds }
        fun autoLoad(autoLoad: Boolean) = apply { this.autoLoad = autoLoad }
        fun autoLoadDelay(autoLoadDelay: Long) = apply { this.autoLoadDelay = autoLoadDelay }
        fun randomInterval(randomInterval: Int) = apply { this.randomInterval = randomInterval }
        fun interstitialMinElapsedSecondsToNextShow(interstitialMinSeconds: Int) =
            apply { this.interstitialMinSeconds = interstitialMinSeconds }

        fun rewardedMinElapsedSecondsToNextShow(rewardedMinSeconds: Int) =
            apply { this.rewardedMinSeconds = rewardedMinSeconds }

        fun addAdPlatform(adPlatform: AdPlatformModel) = apply { this.adPlatforms.add(adPlatform) }
        fun addAdPlatforms(vararg platforms: AdPlatformModel) = apply {
            platforms.forEach {
                this.addAdPlatform(it)
            }
        }

        fun build() = AdManager(this)
    }

}
