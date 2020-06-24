package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.mopub.common.MoPub
import com.mopub.common.MoPubReward
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener
import com.mopub.common.logging.MoPubLog
import com.mopub.mobileads.*
import com.mopub.mobileads.BuildConfig

class MopubAdWrapper(appId: String, activity: Activity, context: Context) : AdPlatformWrapper(appId, activity, context) {
    override val platform: AdPlatformTypeEnum = AdPlatformTypeEnum.MOPUB

    var bannerAdView: MoPubView? = null
    var interstitial: MoPubInterstitial? = null
    var mrecAdView: MoPubView? = null

    companion object {
        var isInitialized = false
    }


    override fun initialize() {
        var unitId: String? = when {
            !interstitialPlacementId.isNullOrEmpty() -> interstitialPlacementId
            !bannerPlacementId.isNullOrEmpty() -> bannerPlacementId
            !rewardedPlacementId.isNullOrEmpty() -> rewardedPlacementId
            !mrecPlacementId.isNullOrEmpty() -> mrecPlacementId
            else -> null
        }

        if (unitId.isNullOrEmpty()) {
            throw Exception("Mopub Initialize Error")
        }

        val sdkConfiguration = SdkConfiguration.Builder(unitId)
        if (BuildConfig.DEBUG) {
            sdkConfiguration.withLogLevel(MoPubLog.LogLevel.DEBUG);
        } else {
            sdkConfiguration.withLogLevel(MoPubLog.LogLevel.INFO);
        }
        MoPub.initializeSdk(context, sdkConfiguration.build(), object : SdkInitializationListener {
            override fun onInitializationFinished() {
            }
        })
    }

    override fun enableTestMode(deviceId: String?) {
        interstitialPlacementId = "24534e1901884e398f1253216226017e"
        bannerPlacementId = "b195f8dd8ded45fe847ad89ed1d016da"
        rewardedPlacementId = "920b6145fb1546cf8b5cf2ac34638bb7"
        mrecPlacementId = "252412d5e9364a05ab77d9396346d73d"
    }

    override fun loadInterstitial(listener: AdPlatformLoadListener?) {
        if (interstitialPlacementId == null) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> null placement id ")
            return
        }

        if (isInterstitialLoaded()) {
            listener?.onLoaded()
            return
        }

        interstitial = MoPubInterstitial(activity, interstitialPlacementId!!)

        interstitial?.interstitialAdListener = object : MoPubInterstitial.InterstitialAdListener {
            override fun onInterstitialLoaded(interstitial: MoPubInterstitial?) {
                listener?.onLoaded()
            }

            override fun onInterstitialShown(interstitial: MoPubInterstitial?) {

            }

            override fun onInterstitialFailed(interstitial: MoPubInterstitial?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> $errorCode")
            }

            override fun onInterstitialDismissed(interstitial: MoPubInterstitial?) {

            }

            override fun onInterstitialClicked(interstitial: MoPubInterstitial?) {

            }
        }

        interstitial?.load()
    }

    override fun showInterstitial(listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noadsloaded")
            return
        }

        interstitial?.interstitialAdListener = object : MoPubInterstitial.InterstitialAdListener {
            override fun onInterstitialLoaded(interstitial: MoPubInterstitial?) {}

            override fun onInterstitialShown(interstitial: MoPubInterstitial?) {
                listener?.onDisplayed()
            }

            override fun onInterstitialFailed(interstitial: MoPubInterstitial?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> $errorCode")
            }

            override fun onInterstitialDismissed(interstitial: MoPubInterstitial?) {
                listener?.onClosed()
            }

            override fun onInterstitialClicked(interstitial: MoPubInterstitial?) {
                listener?.onClicked()
            }
        }

        interstitial?.show()
    }

    override fun isInterstitialLoaded(): Boolean {
        return interstitial?.isReady ?: false

    }

    override fun isBannerLoaded(): Boolean {
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener?) {
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

        bannerAdView = MoPubView(context)
        bannerAdView?.setAdUnitId(bannerPlacementId!!)
        bannerAdView?.adSize = MoPubView.MoPubAdSize.HEIGHT_50

        bannerAdView?.bannerAdListener = object : MoPubView.BannerAdListener {
            override fun onBannerExpanded(banner: MoPubView?) {

            }

            override fun onBannerLoaded(banner: MoPubView) {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView, lp)
                listener?.onDisplayed()
            }

            override fun onBannerCollapsed(banner: MoPubView?) {

            }

            override fun onBannerFailed(banner: MoPubView?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> $errorCode")
            }

            override fun onBannerClicked(banner: MoPubView?) {
                listener?.onClicked()
            }

        }

        bannerAdView?.loadAd()
    }

    override fun loadRewarded(listener: AdPlatformLoadListener?) {

        if (rewardedPlacementId == null) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> null placementid")
            return
        }

        if (isRewardedLoaded()) {
            listener?.onLoaded()
            return
        }


        MoPubRewardedVideos.setRewardedVideoListener(object : MoPubRewardedVideoListener {
            override fun onRewardedVideoClosed(adUnitId: String) {}

            override fun onRewardedVideoCompleted(adUnitIds: MutableSet<String>, reward: MoPubReward) {}

            override fun onRewardedVideoPlaybackError(adUnitId: String, errorCode: MoPubErrorCode) {}

            override fun onRewardedVideoLoadFailure(adUnitId: String, errorCode: MoPubErrorCode) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> $errorCode")
            }

            override fun onRewardedVideoClicked(adUnitId: String) {}

            override fun onRewardedVideoStarted(adUnitId: String) {}

            override fun onRewardedVideoLoadSuccess(adUnitId: String) {
                listener?.onLoaded()
            }
        })
        MoPubRewardedVideos.loadRewardedVideo(rewardedPlacementId!!)
    }

    override fun showRewarded(listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded")
            return
        }

        MoPubRewardedVideos.setRewardedVideoListener(object : MoPubRewardedVideoListener {
            override fun onRewardedVideoClosed(adUnitId: String) {
                listener?.onClosed()
            }

            override fun onRewardedVideoCompleted(adUnitIds: MutableSet<String>, reward: MoPubReward) {
                listener?.onRewarded(reward.label, reward.amount)
            }

            override fun onRewardedVideoPlaybackError(adUnitId: String, errorCode: MoPubErrorCode) {}

            override fun onRewardedVideoLoadFailure(adUnitId: String, errorCode: MoPubErrorCode) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> $errorCode")
            }

            override fun onRewardedVideoClicked(adUnitId: String) {
                listener?.onClicked()
            }

            override fun onRewardedVideoStarted(adUnitId: String) {}

            override fun onRewardedVideoLoadSuccess(adUnitId: String) {
            }
        })
        MoPubRewardedVideos.showRewardedVideo(rewardedPlacementId!!)
    }

    override fun isRewardedLoaded(): Boolean {
        if (rewardedPlacementId == null) return false
        return MoPubRewardedVideos.hasRewardedVideo(rewardedPlacementId!!)
    }

    override fun isMrecLoaded(): Boolean {
        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener?) {
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

        mrecAdView = MoPubView(context)
        mrecAdView?.setAdUnitId(mrecPlacementId!!)
        mrecAdView?.adSize = MoPubView.MoPubAdSize.HEIGHT_250

        mrecAdView?.bannerAdListener = object : MoPubView.BannerAdListener {
            override fun onBannerExpanded(banner: MoPubView?) {

            }

            override fun onBannerLoaded(banner: MoPubView) {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed()
            }

            override fun onBannerCollapsed(banner: MoPubView?) {

            }

            override fun onBannerFailed(banner: MoPubView?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> $errorCode")
            }

            override fun onBannerClicked(banner: MoPubView?) {
                listener?.onClicked()
            }

        }

        mrecAdView?.loadAd()
    }

    override fun destroy() {
        interstitial?.destroy()
        bannerAdView?.destroy()
        mrecAdView?.destroy()
    }

    override fun onCreate() {
        MoPub.onCreate(activity)
    }

    override fun onPause() {
        MoPub.onPause(activity)
    }

    override fun onStop() {
        MoPub.onStop(activity)
    }

    override fun onResume() {
        MoPub.onResume(activity)
    }
}