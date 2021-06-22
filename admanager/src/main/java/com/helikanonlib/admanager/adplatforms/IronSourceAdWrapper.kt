package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.ads.nativetemplates.TemplateView
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

    var viewIntances: MutableMap<String, Any?> = mutableMapOf()
    // var bannerAdView: IronSourceBannerLayout? = null
    // var mrecAdView: IronSourceBannerLayout? = null

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
        if (isInitialized) return
        //IntegrationHelper.validateIntegration(activity);
        IronSource.init(activity, appId, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.BANNER, IronSource.AD_UNIT.REWARDED_VIDEO)
        isInitialized = true
    }

    override fun initialize(context: Context) {

    }

    override fun enableTestMode(deviceId: String?) {

    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
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

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
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
        IronSource.showInterstitial(getPlacementGroupByIndex(placementGroupIndex).interstitial)
    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        return IronSource.isInterstitialReady()
    }

    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as IronSourceBannerLayout? else null
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        var bannerAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as IronSourceBannerLayout? else null

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

        IronSource.loadBanner(bannerAdView, placementName)
        viewIntances[placementName] = bannerAdView
    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
        } else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> load error", platform)
        }
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
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

        IronSource.showRewardedVideo(getPlacementGroupByIndex(placementGroupIndex).rewarded)
    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        return IronSource.isRewardedVideoAvailable()
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val mrecAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as IronSourceBannerLayout? else null

        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        var mrecAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as IronSourceBannerLayout? else null

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
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> isMrecloaded", platform)
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

        IronSource.loadBanner(mrecAdView, placementName)
        viewIntances[placementName] = mrecAdView
    }

    override fun isNativeLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
        return nativeAds.size > 0
    }

    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun showNative(activity: Activity, pos: Int, containerView: ViewGroup, template: TemplateView, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun getNativeAds(activity: Activity, placementGroupIndex: Int): ArrayList<Any> {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        return ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {

        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            var bannerAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(pg.banner)) viewIntances.get(pg.banner) as IronSourceBannerLayout? else null

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


    }


    override fun destroyMrec(activity: Activity) {
        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            var mrecAdView: IronSourceBannerLayout? = if (viewIntances.containsKey(pg.mrec)) viewIntances.get(pg.mrec) as IronSourceBannerLayout? else null

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
    }

    override fun onPause(activity: Activity) {
        IronSource.onPause(activity)
    }

    override fun onResume(activity: Activity) {
        IronSource.onResume(activity)
    }
}