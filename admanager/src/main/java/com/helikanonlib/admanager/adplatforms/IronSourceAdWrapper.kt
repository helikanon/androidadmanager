package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.helikanonlib.admanager.AdPlatformLoadListener
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlib.admanager.AdPlatformTypeEnum
import com.helikanonlib.admanager.AdPlatformWrapper
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.BannerListener
import com.ironsource.mediationsdk.sdk.InterstitialListener
import com.ironsource.mediationsdk.sdk.RewardedVideoListener

class IronSourceAdWrapper(override var appId: String, override var activity: Activity, override var context: Context) :
    AdPlatformWrapper(appId, activity, context) {
    override val platform = AdPlatformTypeEnum.IRONSOURCE

    var bannerAdView: IronSourceBannerLayout? = null
    var mrecAdView: IronSourceBannerLayout? = null

    companion object {
        var isInitialized = false
    }

    override fun initialize() {
        if (isInitialized) return
        //IntegrationHelper.validateIntegration(activity);
        IronSource.init(activity, appId, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.REWARDED_VIDEO)
        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {

    }

    override fun loadInterstitial(listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded()
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onInterstitialAdClosed() {

            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {

            }

            override fun onInterstitialAdClicked() {

            }

            override fun onInterstitialAdReady() {
                listener?.onLoaded()
            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {

            }
        })
        IronSource.loadInterstitial()

    }

    override fun showInterstitial(listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError()
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onInterstitialAdClosed() {
                listener?.onClosed()
            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onInterstitialAdClicked() {
                listener?.onClicked()
            }

            override fun onInterstitialAdReady() {

            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {
                listener?.onDisplayed()
            }
        })

        IronSource.showInterstitial(interstitialPlacementId)
    }

    override fun isInterstitialLoaded(): Boolean {
        return IronSource.isInterstitialReady()
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

        bannerAdView = IronSource.createBanner(activity, ISBannerSize.BANNER)
        bannerAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked()
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onBannerAdLoaded() {
                removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed()
            }

            override fun onBannerAdLeftApplication() {

            }

            override fun onBannerAdScreenDismissed() {

            }

            override fun onBannerAdScreenPresented() {

            }
        }

        IronSource.loadBanner(bannerAdView, bannerPlacementId)
    }

    override fun loadRewarded(listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded()
        } else {
            listener?.onError()
        }
    }

    override fun showRewarded(listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError()
            return
        }

        IronSource.setRewardedVideoListener(object : RewardedVideoListener {
            override fun onRewardedVideoAdClosed() {
                listener?.onClosed()
            }

            override fun onRewardedVideoAdRewarded(p0: Placement?) {
                listener?.onRewarded(p0?.rewardName, p0?.rewardAmount)
            }

            override fun onRewardedVideoAdClicked(p0: Placement?) {
                listener?.onClicked()
            }

            override fun onRewardedVideoAdOpened() {
                listener?.onDisplayed()
            }

            override fun onRewardedVideoAdShowFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onRewardedVideoAvailabilityChanged(available: Boolean) {
                if (available) {
                    //listener?.onLoaded()
                } else {
                    listener?.onError()
                }
            }

            override fun onRewardedVideoAdEnded() {

            }

            override fun onRewardedVideoAdStarted() {

            }
        })

        IronSource.showRewardedVideo(rewardedPlacementId)
    }

    override fun isRewardedLoaded(): Boolean {
        return IronSource.isRewardedVideoAvailable()
    }

    override fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        if (bannerAdView != null) {
            listener?.onError() // ironsource allow only one banner at time
            return
        }

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

        mrecAdView = IronSource.createBanner(activity, ISBannerSize.RECTANGLE)
        mrecAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked()
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                listener?.onError()
            }

            override fun onBannerAdLoaded() {
                removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed()
            }

            override fun onBannerAdLeftApplication() {

            }

            override fun onBannerAdScreenDismissed() {

            }

            override fun onBannerAdScreenPresented() {

            }
        }

        IronSource.loadBanner(mrecAdView, mrecPlacementId)
    }

    override fun destroy() {
        bannerAdView?.let {
            IronSource.destroyBanner(bannerAdView)
        }

        mrecAdView?.let {
            IronSource.destroyBanner(mrecAdView)
        }
    }

    override fun onPause() {
        IronSource.onPause(activity)
    }

    override fun onStop() {

    }

    override fun onResume() {
        IronSource.onResume(activity)
    }
}