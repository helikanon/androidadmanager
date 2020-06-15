package com.helikanonlib.admanager


import android.content.Context
import android.os.Handler
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.*

// TODO FUTURE = add native ads support
// TODO FUTURE = support sort by ad format >> interstitial=facebook,admob,startapp | banner = admob,ironsource,facebook
class AdManager {

    lateinit var activity: AppCompatActivity
    lateinit var context: Context

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

    var lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

    // handlers
    val autoloadInterstitialHandler: Handler = Handler()
    val autoloadRewardedHandler: Handler = Handler()
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    // TODO add option = dont show until date for interstitial

    constructor() {}
    private constructor(builder: AdManager.Builder) {
        this.activity = builder.activity
        this.context = builder.context

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

    fun switchActivity(activity: AppCompatActivity) {
        this.activity = activity
        this.context = this.activity.applicationContext

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.activity = this.activity
            platform.instance.context = this.context
        }
    }

    fun initialize() {
        if (!showAds) return

        initializePlatforms()
        start()
    }

    fun initializePlatforms() {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.initialize()

            if (testMode) {
                platform.instance.enableTestMode(deviceId)
            }
        }
    }

    fun start() {
        if (autoLoad) {
            loadInterstitial()
            loadRewarded()
        }
    }

    fun enableTestMode(deviceId: String) {

        this.testMode = true
        this.deviceId = deviceId

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.initialize()

            if (testMode) {
                platform.instance.enableTestMode(deviceId)
            }
        }
    }

    fun loadInterstitial(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            _loadInterstitialFromFirstAvailable(listener, 0)
        } else {
            _loadInterstitial(listener = listener, platform = platform)
        }
    }

    private fun _loadInterstitialFromFirstAvailable(listener: AdPlatformLoadListener? = null, index: Int = 0) {
        val interstitialAdPlatforms = adPlatforms.filter { it.showInterstitial }
        if (interstitialAdPlatforms.size == 0) {
            return
        }
        val platform = interstitialAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < interstitialAdPlatforms.size) {
                    _loadInterstitialFromFirstAvailable(listener, index + 1)
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
        platform.instance.loadInterstitial(_listener)
    }

    private fun _loadInterstitial(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
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
        platform.instance.loadInterstitial(_listener)
    }

    private fun _showInterstitial(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        val interstitialAdPlatforms = adPlatforms.filter { it.showInterstitial }

        val _listener = object : AdPlatformShowListener() {
            override fun onClosed() {
                // on close load new one for next show
                if (autoLoad) {
                    _autoloadInterstitialByHandler(null, platform)
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
            if (platform.instance.isInterstitialLoaded()) {
                platform.instance.showInterstitial(_listener)
                isShowed = true
            }
        } else {
            run breaker@{
                interstitialAdPlatforms.forEach forEach@{ platform ->
                    if (!platform.instance.isInterstitialLoaded()) {
                        return@forEach
                    }

                    platform.instance.showInterstitial(_listener)
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
                _autoloadInterstitialByHandler(null, platform)
            }
        }
    }

    fun loadAndShowInterstitial(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {

        /*
        call _showInterstitial(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showInterstitial
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded() {
                // this listener will trigger just one time after firt load any platform
                _showInterstitial(listener, platform)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                // it will come here for each ad platforms, so we wants only call _showInterstitial
                // after try all platforms
                // _showInterstitial will trigger user listener
                if (errorMode == AdErrorMode.MANAGER) {
                    _showInterstitial(listener, platform)
                }

            }
        }
        loadInterstitial(loadListener, platform)
    }

    fun showInterstitial(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (autoLoad) {
            _showInterstitial(listener, platform)
        } else {
            loadAndShowInterstitial(listener, platform)
        }
    }

    fun showInterstitialForTimeStrategy(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
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
            showInterstitial(listener, platform)
        }
    }

    private fun _autoloadInterstitialByHandler(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        // load new if there is no ads
        if (hasWorkingAutoloadInterstitialHandler) return

        stopAutoloadInterstitialHandler()
        hasWorkingAutoloadInterstitialHandler = true
        autoloadInterstitialHandler.postDelayed({
            try {
                loadInterstitial(listener, platform)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hasWorkingAutoloadInterstitialHandler = false
        }, autoLoadDelay * 1000)
    }

    fun showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return


        if (platform == null) {
            var startFrom = 0

            // if already banner loaded, start from this platform
            val bannerAdPlatforms = adPlatforms.filter { it.showBanner }
            if (bannerAdPlatforms.size > 0) {
                run breaker@{
                    bannerAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.instance.isBannerLoaded()) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }

            _showBannerFromFirstAvailable(containerView, listener, startFrom)
        } else {
            _showBanner(containerView, listener, platform)
        }
    }

    private fun _showBannerFromFirstAvailable(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0) {
        val bannerAdPlatforms = adPlatforms.filter { it.showBanner }
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
                    _showBannerFromFirstAvailable(containerView, listener, index + 1)
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage) // there is no banner ads. Tried on all platforms
                }
                listener?.onError(errorMode, errorMessage)
            }
        }

        val platform = bannerAdPlatforms[index]
        platform.instance.showBanner(containerView, _listener)
    }

    private fun _showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.instance.showBanner(containerView, object : AdPlatformShowListener() {
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

    fun loadRewarded(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            _loadRewardedFromFirstAvailable(listener)
        } else {
            _loadRewarded(listener, platform)
        }
    }

    private fun _loadRewarded(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
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
        platform.instance.loadRewarded(_listener)
    }

    private fun _loadRewardedFromFirstAvailable(listener: AdPlatformLoadListener? = null, index: Int = 0) {

        val rewardedAdPlatforms = adPlatforms.filter { it.showRewarded }
        if (rewardedAdPlatforms.size == 0) {
            return
        }
        val platform = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < rewardedAdPlatforms.size) {
                    _loadRewardedFromFirstAvailable(listener, index + 1)
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

        platform.instance.loadRewarded(_listener)
    }

    fun _showRewarded(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        val rewardedAdPlatforms = adPlatforms.filter { it.showRewarded }
        val _listener = object : AdPlatformShowListener() {
            override fun onClosed() {
                // on close load new one for next show
                if (autoLoad) {
                    _autoloadRewardedByHandler(null, platform)
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
            if (platform.instance.isRewardedLoaded()) {
                platform.instance.showRewarded(_listener)
                isShowed = true
            }
        } else {
            run breaker@{
                rewardedAdPlatforms.forEach forEach@{ platform ->
                    if (platform.instance.isRewardedLoaded()) {
                        platform.instance.showRewarded(_listener)
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
                _autoloadRewardedByHandler(null, platform)
            }

        }

    }

    fun loadAndShowRewarded(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        /*
        call _showRewarded(listener, platform) in onLoaded and onError because of
        we wants call listener?.onError by _showRewarded
         */
        val loadListener: AdPlatformLoadListener = object : AdPlatformLoadListener() {
            override fun onLoaded() {
                _showRewarded(listener, platform)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    _showRewarded(listener, platform)
                }
            }
        }
        loadRewarded(loadListener, platform)
    }

    fun showRewarded(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (autoLoad) {
            _showRewarded(listener, platform)
        } else {
            loadAndShowRewarded(listener, platform)
        }
    }

    fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            var startFrom = 0

            // if already mrec banner loaded, start from this platform
            val mrecAdPlatforms = adPlatforms.filter { it.showMrec }
            if (mrecAdPlatforms.size > 0) {
                run breaker@{
                    mrecAdPlatforms.forEachIndexed forEachIndexed@{ i, _platform ->
                        if (_platform.instance.isMrecLoaded()) {
                            startFrom = i
                            return@breaker
                        }
                    }
                }
            }

            _showMrecFromFirstAvailable(containerView, listener, startFrom)
        } else {
            _showMrec(containerView, listener, platform)
        }
    }

    private fun _showMrecFromFirstAvailable(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0) {
        val mrecAdPlatforms = adPlatforms.filter { it.showMrec }
        if (mrecAdPlatforms.size == 0) {
            return
        }
        val platform = mrecAdPlatforms[index]

        platform.instance.showMrec(containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed()
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if ((index + 1) < mrecAdPlatforms.size) {
                    _showMrecFromFirstAvailable(containerView, listener, index + 1)
                } else {
                    listener?.onError(AdErrorMode.MANAGER, errorMessage) // not found any ads in all platforms
                }
                listener?.onError(errorMode, errorMessage)
            }
        })
    }

    private fun _showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.instance.showMrec(containerView, object : AdPlatformShowListener() {
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

    private fun _autoloadRewardedByHandler(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel? = null) {
        // load new if there is no ads
        if (hasWorkingAutoloadRewardedHandler) return

        stopAutoloadRewardedHandler()
        hasWorkingAutoloadRewardedHandler = true
        autoloadRewardedHandler.postDelayed({
            try {
                loadRewarded(listener, platform)
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
    }

    fun stopAutoloadRewardedHandler() {
        autoloadRewardedHandler.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        adPlatforms.forEach {
            it.instance.destroy()
        }

        stopAutoloadInterstitialHandler()
        stopAutoloadRewardedHandler()
    }


    fun randInt(min: Int, max: Int): Int {
        return Random().nextInt((max - min) + 1) + min
    }

    data class Builder(
        var activity: AppCompatActivity,
        var context: Context,

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
        fun activity(activity: AppCompatActivity) = apply { this.activity = activity }
        fun context(context: Context) = apply { this.context = context }

        fun enableTestMode(deviceId: String) = apply {
            this.testMode = true
            this.deviceId = deviceId
        }

        fun disableTestMode() = apply { this.testMode = false }
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


    fun onCreate() {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.onCreate()
        }
    }

    fun onResume() {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.onResume()
        }
    }

    fun onPause() {
        if (!showAds) return

        adPlatforms.forEach forEach@{ platform ->
            platform.instance.onPause()
        }
    }
}
