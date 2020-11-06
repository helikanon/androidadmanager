package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
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
        if (isInitialized) return

        MobileAds.initialize(activity.applicationContext)
        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
        bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
        rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
        mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        interstitial = InterstitialAd(activity.applicationContext)
        interstitial?.adUnitId = interstitialPlacementId
        interstitial?.adListener = object : AdListener() {

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> errorcode = $p0", platform)
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                listener?.onLoaded(platform)
            }
        }
        interstitial?.loadAd(AdRequest.Builder().build())
    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded", platform)
            return
        }

        if (listener != null) {
            interstitial?.adListener = object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    listener?.onClicked(platform)
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    listener?.onClosed(platform)
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    listener?.onDisplayed(platform)
                }

            }
        }
        interstitial?.show()
    }

    override fun isInterstitialLoaded(): Boolean {
        return interstitial?.isLoaded ?: false
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
        bannerAdView?.adSize = AdSize.BANNER
        bannerAdView?.adUnitId = bannerPlacementId
        bannerAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=$p0", platform)
            }

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

        rewardedAd = RewardedAd(activity.applicationContext, rewardedPlacementId)
        val adLoadCallback = object : RewardedAdLoadCallback() {
            override fun onRewardedAdLoaded() {
                listener?.onLoaded(platform)
            }

            override fun onRewardedAdFailedToLoad(errorCode: Int) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> errorcode=$errorCode", platform)
            }
        }
        rewardedAd?.loadAd(AdRequest.Builder().build(), adLoadCallback)
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        rewardedAd?.show(activity, object : RewardedAdCallback() {

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

        })
    }

    override fun isRewardedLoaded(): Boolean {
        return rewardedAd?.isLoaded ?: false
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
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> errorcode=$p0", platform)
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
        return nativeAds.size>0
    }

    @JvmOverloads
    override fun loadNativeAds(activity: Activity,count: Int, listener: AdPlatformLoadListener?) {
        lateinit var adLoader: AdLoader

        adLoader = AdLoader.Builder(activity, "ca-app-pub-3940256099942544/2247696110")
            .forUnifiedNativeAd { unifiedNativeAd: UnifiedNativeAd ->
                nativeAds.add(unifiedNativeAd)

                if (!adLoader.isLoading) {
                    listener?.onLoaded(platform)
                }

            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    val a = 1
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val a = 1
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // Methods in the NativeAdOptions.Builder class can be
                    // used here to specify individual options settings.
                    .build()
            )
            .build()
        // adLoader.loadAd(AdRequest.Builder().build())
        adLoader.loadAds(AdRequest.Builder().build(), count)
    }

    override fun showNative(activity: Activity, pos: Int, containerView: RelativeLayout, adSize: String, listener: AdPlatformShowListener?) {
        if (nativeAds.size < (pos + 1)) {
            return
        }

        val nativeAd: UnifiedNativeAd = nativeAds[pos] as UnifiedNativeAd

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

