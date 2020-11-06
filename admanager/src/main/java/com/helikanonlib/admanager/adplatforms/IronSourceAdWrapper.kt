package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.view.View
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.BannerListener
import com.ironsource.mediationsdk.sdk.InterstitialListener
import com.ironsource.mediationsdk.sdk.RewardedVideoListener

class IronSourceAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {
    override val platform = AdPlatformTypeEnum.IRONSOURCE

    var bannerAdView: IronSourceBannerLayout? = null
    var mrecAdView: IronSourceBannerLayout? = null

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
        if (isInitialized) return
        //IntegrationHelper.validateIntegration(activity);
        IronSource.init(activity, appId, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.REWARDED_VIDEO)
        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {

    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >>${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onInterstitialAdClosed() {

            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {

            }

            override fun onInterstitialAdClicked() {

            }

            override fun onInterstitialAdReady() {
                listener?.onLoaded(platform)
            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {

            }
        })
        IronSource.loadInterstitial()

    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noadsloaded", platform)
            return
        }

        IronSource.setInterstitialListener(object : InterstitialListener {
            override fun onInterstitialAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onInterstitialAdClosed() {
                listener?.onClosed(platform)
            }

            override fun onInterstitialAdShowFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onInterstitialAdClicked() {
                listener?.onClicked(platform)
            }

            override fun onInterstitialAdReady() {

            }

            override fun onInterstitialAdOpened() {

            }

            override fun onInterstitialAdShowSucceeded() {
                listener?.onDisplayed(platform)
            }
        })
        IronSource.showInterstitial(interstitialPlacementId)
    }

    override fun isInterstitialLoaded(): Boolean {
        return IronSource.isInterstitialReady()
    }

    override fun isBannerLoaded(): Boolean {
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        val lp =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                .apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                }

        if (_isBannerLoaded(bannerAdView) && !bannerAdView!!.isDestroyed) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                bannerAdView?.visibility = View.VISIBLE
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> isbannerloaded", platform)
            }
            return
        }

        bannerAdView = IronSource.createBanner(activity, ISBannerSize.BANNER)
        bannerAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked(platform)
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onBannerAdLoaded() {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    bannerAdView?.visibility = View.VISIBLE
                    listener?.onDisplayed(platform)
                }
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

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded(platform)
        } else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> load error", platform)
        }
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        IronSource.setRewardedVideoListener(object : RewardedVideoListener {
            override fun onRewardedVideoAdClosed() {
                listener?.onClosed(platform)
            }

            override fun onRewardedVideoAdRewarded(p0: Placement?) {
                listener?.onRewarded(p0?.rewardName, p0?.rewardAmount, platform)
            }

            override fun onRewardedVideoAdClicked(p0: Placement?) {
                listener?.onClicked(platform)
            }

            override fun onRewardedVideoAdOpened() {
                listener?.onDisplayed(platform)
            }

            override fun onRewardedVideoAdShowFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            // TODO debug this method
            /**
             * Invoked when there is a change in the ad availability status.
             *
             * @param - available - value will change to true when rewarded videos are *available.
             *          You can then show the video by calling showRewardedVideo().
             *          Value will change to false when no videos are available.
             */
            // call this after video close
            override fun onRewardedVideoAvailabilityChanged(available: Boolean) {

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

    override fun isMrecLoaded(): Boolean {
        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        if (bannerAdView != null) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> allow only one banner in app. Already a banner created before", platform) // ironsource allow only one banner at time
            return
        }

        val lp =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                .apply {
                    addRule(RelativeLayout.CENTER_HORIZONTAL)
                }

        if (_isBannerLoaded(mrecAdView) && !mrecAdView!!.isDestroyed) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                mrecAdView?.visibility = View.VISIBLE
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> isbannerloaded", platform)
            }
            return
        }

        mrecAdView = IronSource.createBanner(activity, ISBannerSize.RECTANGLE)
        mrecAdView?.bannerListener = object : BannerListener {
            override fun onBannerAdClicked() {
                listener?.onClicked(platform)
            }

            override fun onBannerAdLoadFailed(p0: IronSourceError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> ${p0?.errorCode ?: ""} - ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onBannerAdLoaded() {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    mrecAdView?.visibility = View.VISIBLE
                    listener?.onDisplayed(platform)
                }
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

    override fun isNativeLoaded(): Boolean {
        return nativeAds.size > 0
    }

    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun showNative(activity: Activity, pos: Int, containerView: RelativeLayout, adSize: String, listener: AdPlatformShowListener?) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun destroy(activity: Activity) {
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        if (_isBannerLoaded(bannerAdView) && !bannerAdView!!.isDestroyed) {
            try {
                _removeBannerViewIfExists(bannerAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        bannerAdView?.let {
            IronSource.destroyBanner(bannerAdView)
            bannerAdView = null
        }
    }


    override fun destroyMrec(activity: Activity) {
        if (_isBannerLoaded(mrecAdView) && !mrecAdView!!.isDestroyed) {
            try {
                _removeBannerViewIfExists(mrecAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        mrecAdView?.let {
            IronSource.destroyBanner(mrecAdView)
            mrecAdView = null
        }
    }

    override fun onPause(activity: Activity) {
        IronSource.onPause(activity)
    }

    override fun onResume(activity: Activity) {
        IronSource.onResume(activity)
    }
}