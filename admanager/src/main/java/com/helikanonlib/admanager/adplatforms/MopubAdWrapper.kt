package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.mopub.common.MoPub
import com.mopub.common.MoPubReward
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener
import com.mopub.common.logging.MoPubLog
import com.mopub.mobileads.*
import com.mopub.mobileads.BuildConfig

class MopubAdWrapper(appId: String) : AdPlatformWrapper(appId) {
    override val platform: AdPlatformTypeEnum = AdPlatformTypeEnum.MOPUB

    var bannerAdView: MoPubView? = null
    var interstitial: MoPubInterstitial? = null
    var mrecAdView: MoPubView? = null

    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
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
        MoPub.initializeSdk(activity, sdkConfiguration.build(), object : SdkInitializationListener {
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

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (interstitialPlacementId == null) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> null placement id ", platform)
            return
        }

        if (isInterstitialLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        interstitial = MoPubInterstitial(activity, interstitialPlacementId!!)

        interstitial?.interstitialAdListener = object : MoPubInterstitial.InterstitialAdListener {
            override fun onInterstitialLoaded(interstitial: MoPubInterstitial?) {
                listener?.onLoaded(platform)
            }

            override fun onInterstitialShown(interstitial: MoPubInterstitial?) {

            }

            override fun onInterstitialFailed(interstitial: MoPubInterstitial?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> $errorCode", platform)
            }

            override fun onInterstitialDismissed(interstitial: MoPubInterstitial?) {

            }

            override fun onInterstitialClicked(interstitial: MoPubInterstitial?) {

            }
        }

        interstitial?.load()
    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noadsloaded", platform)
            return
        }

        interstitial?.interstitialAdListener = object : MoPubInterstitial.InterstitialAdListener {
            override fun onInterstitialLoaded(interstitial: MoPubInterstitial?) {}

            override fun onInterstitialShown(interstitial: MoPubInterstitial?) {
                listener?.onDisplayed(platform)
            }

            override fun onInterstitialFailed(interstitial: MoPubInterstitial?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> $errorCode", platform)
            }

            override fun onInterstitialDismissed(interstitial: MoPubInterstitial?) {
                listener?.onClosed(platform)
            }

            override fun onInterstitialClicked(interstitial: MoPubInterstitial?) {
                listener?.onClicked(platform)
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

        bannerAdView = MoPubView(activity.applicationContext)
        bannerAdView?.setAdUnitId(bannerPlacementId!!)
        bannerAdView?.adSize = MoPubView.MoPubAdSize.HEIGHT_50

        bannerAdView?.bannerAdListener = object : MoPubView.BannerAdListener {
            override fun onBannerExpanded(banner: MoPubView?) {

            }

            override fun onBannerLoaded(banner: MoPubView) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onBannerCollapsed(banner: MoPubView?) {

            }

            override fun onBannerFailed(banner: MoPubView?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> $errorCode", platform)
            }

            override fun onBannerClicked(banner: MoPubView?) {
                listener?.onClicked(platform)
            }

        }

        bannerAdView?.loadAd()
    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {

        if (rewardedPlacementId == null) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> null placementid", platform)
            return
        }

        if (isRewardedLoaded()) {
            listener?.onLoaded(platform)
            return
        }


        MoPubRewardedVideos.setRewardedVideoListener(object : MoPubRewardedVideoListener {
            override fun onRewardedVideoClosed(adUnitId: String) {}

            override fun onRewardedVideoCompleted(adUnitIds: MutableSet<String>, reward: MoPubReward) {}

            override fun onRewardedVideoPlaybackError(adUnitId: String, errorCode: MoPubErrorCode) {}

            override fun onRewardedVideoLoadFailure(adUnitId: String, errorCode: MoPubErrorCode) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> $errorCode", platform)
            }

            override fun onRewardedVideoClicked(adUnitId: String) {}

            override fun onRewardedVideoStarted(adUnitId: String) {}

            override fun onRewardedVideoLoadSuccess(adUnitId: String) {
                listener?.onLoaded(platform)
            }
        })
        MoPubRewardedVideos.loadRewardedVideo(rewardedPlacementId!!)
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        MoPubRewardedVideos.setRewardedVideoListener(object : MoPubRewardedVideoListener {
            override fun onRewardedVideoClosed(adUnitId: String) {
                listener?.onClosed(platform)
            }

            override fun onRewardedVideoCompleted(adUnitIds: MutableSet<String>, reward: MoPubReward) {
                listener?.onRewarded(reward.label, reward.amount, platform)
            }

            override fun onRewardedVideoPlaybackError(adUnitId: String, errorCode: MoPubErrorCode) {}

            override fun onRewardedVideoLoadFailure(adUnitId: String, errorCode: MoPubErrorCode) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> $errorCode", platform)
            }

            override fun onRewardedVideoClicked(adUnitId: String) {
                listener?.onClicked(platform)
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

        mrecAdView = MoPubView(activity.applicationContext)
        mrecAdView?.setAdUnitId(mrecPlacementId!!)
        mrecAdView?.adSize = MoPubView.MoPubAdSize.HEIGHT_250

        mrecAdView?.bannerAdListener = object : MoPubView.BannerAdListener {
            override fun onBannerExpanded(banner: MoPubView?) {

            }

            override fun onBannerLoaded(banner: MoPubView) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    listener?.onDisplayed(platform)
                }

            }

            override fun onBannerCollapsed(banner: MoPubView?) {

            }

            override fun onBannerFailed(banner: MoPubView?, errorCode: MoPubErrorCode?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> $errorCode", platform)
            }

            override fun onBannerClicked(banner: MoPubView?) {
                listener?.onClicked(platform)
            }

        }

        mrecAdView?.loadAd()
    }

    override fun destroy(activity: Activity) {
        interstitial?.destroy()

        destroyBanner(activity)

        mrecAdView?.destroy()
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


    override fun onCreate(activity: Activity) {
        MoPub.onCreate(activity)
    }

    override fun onPause(activity: Activity) {
        MoPub.onPause(activity)
    }

    override fun onStop(activity: Activity) {
        MoPub.onStop(activity)
    }

    override fun onResume(activity: Activity) {
        MoPub.onResume(activity)
    }
}