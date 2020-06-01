package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.helikanonlib.admanager.AdPlatformLoadListener
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlib.admanager.AdPlatformTypeEnum
import com.helikanonlib.admanager.AdPlatformWrapper


/**
 * *************************************************************************************************
 * ADMOB ADS HELPER
 * *************************************************************************************************
 */

class AdmobAdWrapper(override var appId: String, override var activity: Activity, override var context: Context) :
    AdPlatformWrapper(appId, activity, context) {

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

    override fun initialize() {
        if (isInitialized) return

        MobileAds.initialize(context)
        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
        bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
        rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
        mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
    }

    override fun loadInterstitial(listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded()
            return
        }

        interstitial = InterstitialAd(context)
        interstitial?.adUnitId = interstitialPlacementId
        interstitial?.adListener = object : AdListener() {

            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                listener?.onLoaded()
            }
        }
        interstitial?.loadAd(AdRequest.Builder().build())
    }

    override fun showInterstitial(listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError()
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

    override fun showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }

        if (isBannerLoaded(bannerAdView)) {
            try {
                removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError()
            }
            return
        }

        bannerAdView = AdView(context)
        bannerAdView?.adSize = AdSize.BANNER
        bannerAdView?.adUnitId = bannerPlacementId
        bannerAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed()
            }

            override fun onAdClicked() {
                listener?.onClicked()
            }
        }
        bannerAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun loadRewarded(listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded()
            return
        }

        rewardedAd = RewardedAd(context, rewardedPlacementId)
        val adLoadCallback = object : RewardedAdLoadCallback() {
            override fun onRewardedAdLoaded() {
                listener?.onLoaded()
            }

            override fun onRewardedAdFailedToLoad(errorCode: Int) {
                listener?.onError()
            }
        }
        rewardedAd?.loadAd(AdRequest.Builder().build(), adLoadCallback)
    }

    override fun showRewarded(listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError()
            return
        }

        rewardedAd?.show(activity, object : RewardedAdCallback() {

            override fun onRewardedAdFailedToShow(p0: Int) {
                super.onRewardedAdFailedToShow(p0)
                listener?.onError()
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

    override fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener?) {

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }

        if (isBannerLoaded(mrecAdView)) {
            try {
                removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError()
            }
            return
        }

        mrecAdView = AdView(context)
        mrecAdView?.adSize = AdSize.MEDIUM_RECTANGLE
        mrecAdView?.adUnitId = mrecPlacementId
        mrecAdView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                listener?.onError()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)

                listener?.onDisplayed()
            }

            override fun onAdClicked() {
                listener?.onClicked()
            }
        }
        mrecAdView?.loadAd(AdRequest.Builder().build());

    }

    override fun destroy() {
        interstitial = null
        bannerAdView = null
        rewardedAd = null
    }

    override fun onPause() {}
    override fun onStop() {}
    override fun onResume() {}

}

