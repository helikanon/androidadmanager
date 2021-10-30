package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.admanager.*


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
                    listener?.onLoaded(platform)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    viewIntances.put(placementName, null)
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${adError.code} / ${adError.message}", platform)
                }
            })

    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
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
    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: InterstitialAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as InterstitialAd? else null
        return interstitial != null
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
        return rewardedAd != null
    }


    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: AdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdView? else null
        return _isBannerLoaded(bannerAdView)
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
        bannerAdView?.adSize = getBannerAdaptiveSize(activity, containerView)
        bannerAdView?.adUnitId = placementName
        bannerAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=${error.code} / ${error.message}", platform)
            }
            /*override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=$p0", platform)
            }*/

            override fun onAdLoaded() {
                super.onAdLoaded()
                activity.runOnUiThread {
                    viewIntances[placementName] = bannerAdView

                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        bannerAdView?.loadAd(AdRequest.Builder().build())
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val mrecAdView: AdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdView? else null
        return _isBannerLoaded(mrecAdView)
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
        mrecAdView?.adSize = AdSize.MEDIUM_RECTANGLE
        mrecAdView?.adUnitId = placementName
        mrecAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> errorcode=${error.code} / ${error.message}", platform)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                activity.runOnUiThread {
                    viewIntances[instanceKeyName] = mrecAdView

                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        mrecAdView?.loadAd(AdRequest.Builder().build())

    }

    override fun isNativeLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
        return nativeAds.size > 0
    }

    @JvmOverloads
    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {

        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
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

    override fun showNative(activity: Activity, pos: Int, listener: AdPlatformShowListener?, placementGroupIndex: Int): NativeAd? {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()

        if (nativeAds.size < (pos + 1)) {
            return null
        }

        //val nativeAd: NativeAd = nativeAds[pos] as NativeAd
        return nativeAds[pos] as NativeAd

        /*val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(
            if (adSize == "small") {
                R.layout.admob_native_small_template
            } else {
                R.layout.admob_native_medium_template
            }, null
        )

        val template = v.findViewById<TemplateView>(
            if (adSize == "small") {
                R.id.admanager_native_small
            } else {
                R.id.admanager_native_medium
            }
        )*/

        /*val styles = NativeTemplateStyle.Builder().build()
        template.setStyles(styles)
        template.setNativeAd(nativeAd)
        containerView.addView(template.parent as ViewGroup)*/


    }

    override fun getNativeAds(activity: Activity, placementGroupIndex: Int): ArrayList<Any> {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        return if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {

        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            viewIntances.put(pg.interstitial, null)
            viewIntances.put(pg.rewarded, null)

            // destroy NATIVE ads
            var nativeAds: ArrayList<Any> = if (viewIntances.containsKey(pg.native) && viewIntances[pg.native] != null) viewIntances.get(pg.native) as ArrayList<Any> else ArrayList<Any>()
            try {
                nativeAds.forEach {
                    (it as NativeAd).destroy()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                nativeAds.clear()
                viewIntances.put(pg.native, nativeAds)
            }
        }

        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {

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


    }

    override fun destroyMrec(activity: Activity) {

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

    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}

