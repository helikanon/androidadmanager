package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.R
import com.helikanonlib.admanager.admobnativetemplates.NativeTemplateStyle
import com.helikanonlib.admanager.admobnativetemplates.TemplateView

/**
 * *************************************************************************************************
 * ADMOB ADS HELPER
 * *************************************************************************************************
 */

class AdmobAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    override var platform = AdPlatformTypeEnum.ADMOB
    var viewIntances: MutableMap<String, Any?> = mutableMapOf()

    /*var bannerAdView: AdView? = null
    var interstitial: InterstitialAd? = null
    var rewardedAd: RewardedAd? = null
    var mrecAdView: AdView? = null*/

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {
    }

    override fun initialize(context: Context, testMode: Boolean) {
        if (isInitialized) return

        MobileAds.initialize(context)
        isInitialized = true

        if (testMode) {
            enableTestMode(context, null)
        }
    }

    override fun enableTestMode(context: Context, deviceId: String?) {

        for (group in placementGroups) {
            group.interstitial = "ca-app-pub-3940256099942544/1033173712"
            group.banner = "ca-app-pub-3940256099942544/6300978111"
            group.rewarded = "ca-app-pub-3940256099942544/5224354917"
            group.mrec = "ca-app-pub-3940256099942544/6300978111"
            group.native = "ca-app-pub-3940256099942544/2247696110"
            group.appOpenAd = "ca-app-pub-3940256099942544/3419835294"
        }

    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        viewIntances.put(placementName, null)

        InterstitialAd.load(
            activity, placementName, AdRequest.Builder()
                .build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    viewIntances.put(placementName, interstitialAd)
                    updateLastLoadInterstitialDateByAdPlatform(platform)

                    listener?.onLoaded(platform)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    viewIntances.put(placementName, null)
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${adError.code} / ${adError.message}", platform)
                }
            })
    }

    override fun showInterstitial(activity: Activity, shownWhere: String, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: InterstitialAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as InterstitialAd? else null

        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial show >> error code=${adError.code} / ${adError.message}", platform)
            }

            override fun onAdShowedFullScreenContent() {
                listener?.onDisplayed(platform)
            }

            override fun onAdDismissedFullScreenContent() {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

        }

        interstitial?.show(activity)
        viewIntances[placementName] = null
    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: InterstitialAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as InterstitialAd? else null

        var isLoaded = interstitial != null
        if (isLoaded && !isValidLoadedInterstitial(platform)) {
            viewIntances.put(placementName, null)
            isLoaded = false
        }

        return isLoaded
    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        viewIntances.put(placementName, null)

        RewardedAd.load(
            activity, placementName, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    viewIntances[placementName] = p0
                    updateLastLoadRewardedDateByAdPlatform(platform)

                    listener?.onLoaded(platform)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    viewIntances.put(placementName, null)
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded load >> error code=${adError.code} / ${adError.message}", platform)
                }
            })

    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewardedAd: RewardedAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as RewardedAd? else null

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded show >> error code=${adError.code} / ${adError.message}", platform)
                viewIntances[placementName] = null
            }

            override fun onAdShowedFullScreenContent() {
                listener?.onDisplayed(platform)
            }

            override fun onAdDismissedFullScreenContent() {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

        }

        rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
            listener?.onRewarded(rewardItem.type, rewardItem.amount, platform)
        })
    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewardedAd: RewardedAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as RewardedAd? else null

        var isLoaded = rewardedAd != null
        if (isLoaded && !isValidLoadedRewarded(platform)) {
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }


    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: AdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdView? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        if (isLoaded && !isValidLoadedBanner(platform)) {
            _removeBannerViewIfExists(bannerAdView)
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    fun getBannerAdaptiveSize(activity: Activity, containerView: RelativeLayout): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = containerView.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {

        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        var bannerAdView: AdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdView? else null

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> isbannerloaded", platform)
            }
            return
        }

        bannerAdView = AdView(activity.applicationContext)
        // bannerAdView?.adSize = AdSize.SMART_BANNER

        bannerAdView.setAdSize(getBannerAdaptiveSize(activity, containerView))
        bannerAdView.adUnitId = placementName
        bannerAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                viewIntances[placementName] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=${error.code} / ${error.message}", platform)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                viewIntances[placementName] = bannerAdView
            }

            override fun onAdImpression() {
                super.onAdImpression()

                listener?.onDisplayed(platform)
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        _removeBannerViewIfExists(bannerAdView, containerView)
        containerView.addView(bannerAdView, lp)

        bannerAdView?.loadAd(AdRequest.Builder().build())
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val instanceKeyName = placementName + "_mrec"
        val mrecAdView: AdView? = if (viewIntances.containsKey(instanceKeyName)) viewIntances.get(instanceKeyName) as AdView? else null


        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedBanner(platform)) {
            _removeBannerViewIfExists(mrecAdView)
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {

        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val instanceKeyName = placementName + "_mrec"
        var mrecAdView: AdView? = if (viewIntances.containsKey(instanceKeyName)) viewIntances.get(instanceKeyName) as AdView? else null

        val lp =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                .apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                }

        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> isbannerloaded", platform)
            }
            return
        }

        mrecAdView = AdView(activity.applicationContext)
        // mrecAdView?.adSize = AdSize.MEDIUM_RECTANGLE
        mrecAdView.setAdSize(getMrecBannerAdaptiveSize(activity, containerView))
        mrecAdView.adUnitId = placementName
        mrecAdView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)

                viewIntances[placementName] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> errorcode=${error.code} / ${error.message}", platform)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                viewIntances[instanceKeyName] = mrecAdView
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView, lp)

        mrecAdView.loadAd(AdRequest.Builder().build())

    }

    fun getMrecBannerAdaptiveSize(activity: Activity, containerView: RelativeLayout): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = containerView.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt() - 24

        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
    }

    override fun hasLoadedNative(nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): Boolean {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }
        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
        return nativeAds.size > 0
    }

    @JvmOverloads
    override fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {

        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }
        var nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()


        // destroy olds
        try {
            nativeAds.forEach {
                (it as NativeAd).destroy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            nativeAds.clear()
            viewIntances.put(placementName, nativeAds)
        }

        lateinit var adLoader: AdLoader
        adLoader = AdLoader.Builder(activity, placementName)
            .forNativeAd { nativeAd ->

                nativeAds.add(nativeAd)
                viewIntances.put(placementName, nativeAds)

                if (!adLoader.isLoading) {
                    listener?.onLoaded(platform)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (!adLoader.isLoading) {
                        if (nativeAds.size > 0) {
                            listener?.onLoaded(platform)
                        } else {
                            listener?.onError(AdErrorMode.PLATFORM, adError.message, platform)
                        }
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .build()
            )
            .build()
        // adLoader.loadAd(AdRequest.Builder().build())
        adLoader.loadAds(AdRequest.Builder().build(), count)
    }

    private var lastLoadedNativeAdPositions: MutableMap<String, Int> = mutableMapOf()

    override fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener?, placementGroupIndex: Int): Boolean {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }
        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()

        val lastLoadedNativeAdPosition: Int = if (lastLoadedNativeAdPositions.containsKey(placementName)) lastLoadedNativeAdPositions[placementName]!! else -1
        var showPositionAt = if (lastLoadedNativeAdPosition == -1) 0 else lastLoadedNativeAdPosition + 1

        if (showPositionAt >= nativeAds.size) {
            showPositionAt = 0
        }

        if (nativeAds.size == 0) {
            return false
        }
        lastLoadedNativeAdPositions[placementName] = showPositionAt

        val nativeAd = nativeAds[showPositionAt] as NativeAd

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val nativeView = inflater.inflate(
            if (nativeAdFormat == AdFormatEnum.NATIVE) {
                R.layout.admob_native_small
            } else {
                R.layout.admob_native_medium
            }, null
        )


        val template = nativeView.findViewById<TemplateView>(
            if (nativeAdFormat == AdFormatEnum.NATIVE) {
                R.id.admanager_native_small
            } else {
                R.id.admanager_native_medium
            }
        )

        val styles = NativeTemplateStyle.Builder().build()
        template.setStyles(styles)
        template.setNativeAd(nativeAd)

        _removeViewIfExists(template.parent as ViewGroup?, containerView)
        containerView.addView(template.parent as ViewGroup)

        return true
    }

    override fun getNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): ArrayList<Any> {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }
        return if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {

        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]
                viewIntances.put(pg.interstitial, null)
                viewIntances.put(pg.rewarded, null)

                // destroy NATIVE ads
                _destroyAllNativeAds(pg.native)
                _destroyAllNativeAds(pg.nativeMedium)

            }

            destroyBanner(activity)
            destroyMrec(activity)
        } catch (e: Exception) {
            Log.e("Admob", e.message ?: "")
        }
    }

    private fun _destroyAllNativeAds(placementName: String) {
        try {
            // destroy NATIVE ads
            var nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
            try {
                nativeAds.forEach {
                    (it as NativeAd).destroy()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                nativeAds.clear()
                viewIntances.put(placementName, nativeAds)
            }
        } catch (e: Exception) {
            Log.e("Admob", e.message ?: "")
        }
    }

    override fun destroyBanner(activity: Activity) {
        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]

                var bannerAdView: AdView? = if (viewIntances.containsKey(pg.banner)) viewIntances.get(pg.banner) as AdView? else null
                if (_isBannerLoaded(bannerAdView)) {
                    try {
                        _removeBannerViewIfExists(bannerAdView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                bannerAdView?.destroy()
                bannerAdView = null
                viewIntances[pg.banner] = null
            }
        } catch (e: Exception) {
            Log.e("Admob", e.message ?: "")
        }


    }

    override fun destroyMrec(activity: Activity) {
        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]

                var mrecAdView: AdView? = if (viewIntances.containsKey(pg.mrec)) viewIntances.get(pg.mrec) as AdView? else null
                if (_isBannerLoaded(mrecAdView)) {
                    try {
                        _removeBannerViewIfExists(mrecAdView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                mrecAdView?.destroy()
                mrecAdView = null
                viewIntances[pg.mrec] = null
            }
        } catch (e: Exception) {
            Log.e("Admob", e.message ?: "")
        }

    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}
}

