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
    var lastShowDateByAdFormat = mutableMapOf<AdFormatEnum, Date>()

    // handlers
    val autoloadInterstitialHandler: Handler = Handler()
    val autoloadRewardedHandler: Handler = Handler()
    private var hasWorkingAutoloadInterstitialHandler = false
    private var hasWorkingAutoloadRewardedHandler = false

    // TODO add dont show until interstitial ability

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
        loadInterstitial()
        loadRewarded()
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
            override fun onError() {
                if (autoLoad) {
                    if ((index + 1) < interstitialAdPlatforms.size) {
                        _loadInterstitialFromFirstAvailable(listener, index + 1)
                    }
                }
                listener?.onError()
            }

            override fun onLoaded() {
                listener?.onLoaded()
            }
        }
        platform.instance.loadInterstitial(_listener)
    }

    private fun _loadInterstitial(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError() {
                listener?.onError()
            }

            override fun onLoaded() {
                listener?.onLoaded()
            }

        }
        platform.instance.loadInterstitial(_listener)
    }

    fun showInterstitialForTimeStrategy(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        var isAvailableToShow = true
        var lastShowDate = lastShowDateByAdFormat.get(AdFormatEnum.INTERSTITIAL)

        if (lastShowDate != null) {
            val now = Date()
            val elapsedSeconds = (now.time - lastShowDate.time) / 1000
            val requiredElapsedTime = randInt(0, randomInterval) + interstitialMinElapsedSecondsToNextShow
            isAvailableToShow = elapsedSeconds > requiredElapsedTime
        }

        if (isAvailableToShow) {
            showInterstitial(listener, platform)
        }
    }

    fun showInterstitial(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
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

            override fun onError() {
                globalInterstitialShowListener?.onError()
                listener?.onError()
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
            globalInterstitialShowListener?.onError()
            listener?.onError()
            if (autoLoad) {
                _autoloadInterstitialByHandler(null, platform)
            }
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
            _showBannerFromFirstAvailable(containerView, listener)
        } else {
            _showBanner(containerView, listener, platform)
        }
    }

    private fun _showBannerFromFirstAvailable(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, index: Int = 0) {
        val bannerAdPlatforms = adPlatforms.filter { it.showBanner }
        if (bannerAdPlatforms.size == 0) {
            return
        }
        val platform = bannerAdPlatforms[index]

        platform.instance.showBanner(containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed()
            }

            override fun onError() {
                if ((index + 1) < bannerAdPlatforms.size)
                    _showBannerFromFirstAvailable(containerView, listener, index + 1)
            }
        })
    }

    private fun _showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.instance.showBanner(containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.BANNER)
                listener?.onDisplayed()
            }

            override fun onError() {
                listener?.onError()
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

    private fun _loadRewardedFromFirstAvailable(listener: AdPlatformLoadListener? = null, index: Int = 0) {

        val rewardedAdPlatforms = adPlatforms.filter { it.showRewarded }
        if (rewardedAdPlatforms.size == 0) {
            return
        }
        val platform = rewardedAdPlatforms[index]

        val _listener = object : AdPlatformLoadListener() {
            override fun onError() {
                if ((index + 1) < rewardedAdPlatforms.size) {
                    _loadRewardedFromFirstAvailable(listener, index + 1)
                }

                listener?.onError()
            }

            override fun onLoaded() {
                listener?.onLoaded()
            }
        }

        platform.instance.loadRewarded(_listener)
    }

    private fun _loadRewarded(listener: AdPlatformLoadListener? = null, platform: AdPlatformModel) {
        val _listener = object : AdPlatformLoadListener() {
            override fun onError() {
                listener?.onError()
            }

            override fun onLoaded() {
                listener?.onLoaded()
            }

        }
        platform.instance.loadRewarded(_listener)
    }

    fun showRewarded(listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
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

            override fun onError() {
                globalRewardedShowListener?.onError()
                listener?.onError()
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
            globalRewardedShowListener?.onError()
            listener?.onError()
            if (autoLoad) {
                _autoloadRewardedByHandler(null, platform)
            }

        }

    }

    fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel? = null) {
        if (!showAds) return

        if (platform == null) {
            _showMrecFromFirstAvailable(containerView, listener)
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

            override fun onError() {
                if ((index + 1) < mrecAdPlatforms.size)
                    _showMrecFromFirstAvailable(containerView, listener, index + 1)
            }
        })
    }

    private fun _showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener? = null, platform: AdPlatformModel) {

        platform.instance.showMrec(containerView, object : AdPlatformShowListener() {
            override fun onDisplayed() {
                saveLastShowDate(AdFormatEnum.MREC)
                listener?.onDisplayed()
            }

            override fun onError() {
                listener?.onError()
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
        /*when (adFormatEnum) {
            AdFormatEnum.BANNER -> {
                lastShowDateByAdFormat.put(AdFormatEnum.BANNER, Date())
            }
            AdFormatEnum.INTERSTITIAL -> {
                lastShowDateByAdFormat.put(AdFormatEnum.INTERSTITIAL, Date())
            }
            AdFormatEnum.REWARDED -> {
                lastShowDateByAdFormat.put(AdFormatEnum.REWARDED, Date())
            }
        }*/

        /*val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val prefsEditor:SharedPreferences.Editor = prefs.edit()
        prefsEditor.putString(key,"")*/
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
        fun interstitialMinElapsedSecondsToNextShow(interstitialMinSeconds: Int) = apply { this.interstitialMinSeconds = interstitialMinSeconds }
        fun rewardedMinElapsedSecondsToNextShow(rewardedMinSeconds: Int) = apply { this.rewardedMinSeconds = rewardedMinSeconds }

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
