package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.R


/**
 * *************************************************************************************************
 * ADMOB ADS HELPER
 * *************************************************************************************************
 */

class AdmobAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    override var platform = AdPlatformTypeEnum.ADMOB
    /*override var interstitialPlacementId: String? = null
    override var bannerPlacementId: String? = null
    override var rewardedPlacementId: String? = null
    override var mrecPlacementId: String? = null*/

    var bannerAdView: AdView? = null
    var interstitial: InterstitialAd? = null
    var rewardedAd: RewardedAd? = null
    var mrecAdView: AdView? = null

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
    }

    override fun initialize(context: Context) {
        if (isInitialized) return

        MobileAds.initialize(context)
        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
        bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
        rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
        mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
        nativePlacementId = "ca-app-pub-3940256099942544/2247696110"
        appOpenAdPlacementId = "ca-app-pub-3940256099942544/3419835294"
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        interstitial = null
        InterstitialAd.load(
            activity, interstitialPlacementId, AdRequest.Builder()
                .build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    listener?.onLoaded(platform)
                    interstitial = interstitialAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${adError.code} / ${adError.message}", platform)
                }
            })

    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded", platform)
            return
        }

        if (listener != null) {

            interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial show >> error code=${adError.code} / ${adError.message}", platform)
                    interstitial = null
                }

                override fun onAdShowedFullScreenContent() {
                    listener?.onDisplayed(platform)
                    interstitial = null
                }

                override fun onAdDismissedFullScreenContent() {
                    listener?.onClosed(platform)
                }

            }

        }
        interstitial?.show(activity)
    }

    override fun isInterstitialLoaded(): Boolean {
        return interstitial != null
    }

    override fun isBannerLoaded(): Boolean {
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
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
        bannerAdView?.adSize = AdSize.SMART_BANNER
        bannerAdView?.adUnitId = bannerPlacementId
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
                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        bannerAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        rewardedAd = null
        RewardedAd.load(
            activity, rewardedPlacementId, AdRequest.Builder()
                .build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(p0: RewardedAd) {
                    rewardedAd = p0
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded load >> error code=${adError.code} / ${adError.message}", platform)
                }
            })

    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        /*rewardedAd?.show(activity, object : RewardedAdCallback() {

            override fun onRewardedAdFailedToShow(p0: Int) {
                super.onRewardedAdFailedToShow(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> errorcode=$p0", platform)
            }

            override fun onRewardedAdClosed() {
                super.onRewardedAdClosed()
                listener?.onClosed(platform)
            }

            override fun onRewardedAdOpened() {
                super.onRewardedAdOpened()
                listener?.onDisplayed(platform)
            }

            override fun onUserEarnedReward(p0: RewardItem) {
                listener?.onRewarded(p0.type, p0.amount, platform)
            }

        })*/

        if (listener != null) {

            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded show >> error code=${adError.code} / ${adError.message}", platform)
                    rewardedAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    listener?.onDisplayed(platform)
                    rewardedAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    listener?.onClosed(platform)
                }

            }
        }

        rewardedAd?.show(activity, OnUserEarnedRewardListener() {
            fun onUserEarnedReward(rewardItem: RewardItem) {
                listener?.onRewarded(rewardItem.type, rewardItem.amount, platform)
            }
        })
    }

    override fun isRewardedLoaded(): Boolean {
        return rewardedAd != null
    }

    override fun isMrecLoaded(): Boolean {
        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

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
        mrecAdView?.adUnitId = mrecPlacementId
        mrecAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> errorcode=${error.code} / ${error.message}", platform)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onAdClicked() {
                listener?.onClicked(platform)
            }
        }
        mrecAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun isNativeLoaded(): Boolean {
        return nativeAds.size > 0
    }

    @JvmOverloads
    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?) {
        lateinit var adLoader: AdLoader

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

        adLoader = AdLoader.Builder(activity, nativePlacementId)
            .forNativeAd { nativeAd ->
                nativeAds.add(nativeAd)

                if (!adLoader.isLoading) {
                    listener?.onLoaded(platform)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (!adLoader.isLoading) {
                        listener?.onError(AdErrorMode.PLATFORM, adError.message, platform)
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

    override fun showNative(activity: Activity, pos: Int, containerView: ViewGroup, adSize: String, listener: AdPlatformShowListener?) {
        if (nativeAds.size < (pos + 1)) {
            return
        }

        val nativeAd: NativeAd = nativeAds[pos] as NativeAd
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
        )
        val styles = NativeTemplateStyle.Builder().build()
        template.setStyles(styles)
        template.setNativeAd(nativeAd)
        containerView.addView(v)


    }

    override fun destroy(activity: Activity) {
        interstitial = null
        destroyBanner(activity)

        rewardedAd = null
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bannerAdView?.destroy()
        bannerAdView = null
    }


    override fun destroyMrec(activity: Activity) {
        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mrecAdView?.destroy()
        mrecAdView = null
    }

    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}

