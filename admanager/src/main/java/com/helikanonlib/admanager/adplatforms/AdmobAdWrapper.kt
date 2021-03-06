package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.admanager.*


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
            listener?.onLoaded()
            return
        }

        interstitial = InterstitialAd(activity.applicationContext)
        interstitial?.adUnitId = interstitialPlacementId
        interstitial?.adListener = object : AdListener() {

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> errorcode = $p0")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                listener?.onLoaded()
            }
        }
        interstitial?.loadAd(AdRequest.Builder().build())
    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded")
            return
        }

        if (listener != null) {
            interstitial?.adListener = object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    listener?.onClicked()
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    listener?.onClosed()
                }

                override fun onAdOpened() {
                    super.onAdOpened()
                    listener?.onDisplayed()
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
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> isbannerloaded")
            }
            return
        }

        bannerAdView = AdView(activity.applicationContext)
        bannerAdView?.adSize = AdSize.BANNER
        bannerAdView?.adUnitId = bannerPlacementId
        bannerAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=$p0")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed()
            }

            override fun onAdClicked() {
                listener?.onClicked()
            }
        }
        bannerAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded()
            return
        }

        rewardedAd = RewardedAd(activity.applicationContext, rewardedPlacementId)
        val adLoadCallback = object : RewardedAdLoadCallback() {
            override fun onRewardedAdLoaded() {
                listener?.onLoaded()
            }

            override fun onRewardedAdFailedToLoad(errorCode: Int) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> errorcode=$errorCode")
            }
        }
        rewardedAd?.loadAd(AdRequest.Builder().build(), adLoadCallback)
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded")
            return
        }

        rewardedAd?.show(activity, object : RewardedAdCallback() {

            override fun onRewardedAdFailedToShow(p0: Int) {
                super.onRewardedAdFailedToShow(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> errorcode=$p0")
            }

            override fun onRewardedAdClosed() {
                super.onRewardedAdClosed()
                listener?.onClosed()
            }

            override fun onRewardedAdOpened() {
                super.onRewardedAdOpened()
                listener?.onDisplayed()
            }

            override fun onUserEarnedReward(p0: RewardItem) {
                listener?.onRewarded(p0.type, p0.amount)
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
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> isbannerloaded")
            }
            return
        }

        mrecAdView = AdView(activity.applicationContext)
        mrecAdView?.adSize = AdSize.MEDIUM_RECTANGLE
        mrecAdView?.adUnitId = mrecPlacementId
        mrecAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> errorcode=$p0")
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)

                listener?.onDisplayed()
            }

            override fun onAdClicked() {
                listener?.onClicked()
            }
        }
        mrecAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun destroy(activity: Activity) {
        interstitial = null
        destroyBanner(activity)
        rewardedAd = null
        mrecAdView = null
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

