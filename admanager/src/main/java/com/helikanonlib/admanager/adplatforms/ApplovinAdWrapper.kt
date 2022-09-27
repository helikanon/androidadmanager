package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.applovin.sdk.AppLovinSdkUtils
import com.google.android.gms.ads.nativead.NativeAd
import com.helikanonlib.admanager.*

class ApplovinAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    override var platform = AdPlatformTypeEnum.APPLOVIN
    var viewIntances: MutableMap<String, Any?> = mutableMapOf()


    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {


    }

    override fun initialize(context: Context, testMode: Boolean) {
        if (isInitialized) return


        AppLovinSdk.getInstance(context).mediationProvider = "max"
        AppLovinSdk.getInstance(context).initializeSdk { configuration: AppLovinSdkConfiguration ->

        }
        isInitialized = true

    }

    override fun enableTestMode(context: Context, deviceId: String?) {
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        viewIntances.put(placementName, null)

        val applovinInterstitialIns = MaxInterstitialAd(placementName, activity)
        applovinInterstitialIns.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                updateLastLoadInterstitialDateByAdPlatform(platform)
                listener?.onLoaded(platform)
            }


            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${error?.code} / ${error?.message}", platform)
            }

            override fun onAdDisplayed(ad: MaxAd?) {

            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

        })

        applovinInterstitialIns.loadAd()
        viewIntances.put(placementName, applovinInterstitialIns)

    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: MaxInterstitialAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxInterstitialAd? else null

        interstitial?.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd?) {

            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platform)
            }

            override fun onAdHidden(ad: MaxAd?) {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platform)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial show >> error code=${error?.code} / ${error?.message}", platform)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial show >> error code=${error?.code} / ${error?.message}", platform)
            }

        })
        interstitial?.showAd()
        viewIntances[placementName] = null // gösterir göstermez boşalt

    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: MaxInterstitialAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxInterstitialAd? else null

        var isLoaded = interstitial?.isReady ?: false
        if (isLoaded && !isValidLoadedInterstitial(platform)) {
            viewIntances.put(placementName, null)
            isLoaded = false
        }

        return isLoaded
    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        viewIntances.put(placementName, null)

        val rewardedAd = MaxRewardedAd.getInstance(placementName, activity)

        rewardedAd?.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                updateLastLoadRewardedDateByAdPlatform(platform)

                listener?.onLoaded(platform)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded load >> error code=${error?.code} / ${error?.message}", platform)
            }


            override fun onAdDisplayed(ad: MaxAd?) {

            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {

            }

            override fun onRewardedVideoCompleted(ad: MaxAd?) {

            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {

            }

        })
        rewardedAd?.loadAd()
        viewIntances[placementName] = rewardedAd
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewardedAd: MaxRewardedAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxRewardedAd? else null

        rewardedAd?.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd?) {

            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platform)
            }

            override fun onAdHidden(ad: MaxAd?) {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

            override fun onAdClicked(ad: MaxAd?) {
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {

            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded show >> error code=${error?.code} / ${error?.message}", platform)
                viewIntances[placementName] = null
            }

            override fun onRewardedVideoStarted(ad: MaxAd?) {

            }

            override fun onRewardedVideoCompleted(ad: MaxAd?) {

            }

            override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
                listener?.onRewarded(reward?.label, reward?.amount, platform)
            }
        })

        rewardedAd?.showAd()
        viewIntances[placementName] = null

    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewardedAd: MaxRewardedAd? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxRewardedAd? else null

        var isLoaded = rewardedAd != null
        if (isLoaded && !isValidLoadedRewarded(platform)) {
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: MaxAdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxAdView? else null

        var isLoaded = _isBannerLoaded(bannerAdView)
        if (isLoaded && !isValidLoadedBanner(platform)) {
            _removeBannerViewIfExists(bannerAdView)
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        var bannerAdView: MaxAdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxAdView? else null

        /*val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }*/

        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView, containerView)
                containerView.addView(bannerAdView)
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> isbannerloaded", platform)
            }
            return
        }

        bannerAdView = MaxAdView(placementName, activity)

        val heightDp = MaxAdFormat.BANNER.getAdaptiveSize(activity).height
        val heightPx = AppLovinSdkUtils.dpToPx(activity, heightDp)
        bannerAdView.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
        bannerAdView.setExtraParameter("adaptive_banner", "true")
        // applovinBannerIns?.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.resources.getDimensionPixelSize(R.dimen.applovinBannerHeight))
        //applovinBannerIns?.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, activity.resources.getDimensionPixelSize(R.dimen.applovinBannerHeight))


        bannerAdView.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                viewIntances[placementName] = bannerAdView
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platform)
            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platform)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                viewIntances[placementName] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=${error?.code} / ${error?.message}", platform)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onAdExpanded(ad: MaxAd?) {

            }

            override fun onAdCollapsed(ad: MaxAd?) {

            }

        })
        _removeBannerViewIfExists(bannerAdView, containerView)
        containerView.addView(bannerAdView)
        // listener?.onDisplayed(AdPlatformTypeEnum.APPLOVIN)

        bannerAdView.loadAd()
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val mrecAdView: MaxAdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxAdView? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedBanner(platform)) {
            _removeBannerViewIfExists(mrecAdView)
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        var mrecAdView: MaxAdView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as MaxAdView? else null

        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView, containerView)
                containerView.addView(mrecAdView)
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> ismrecloaded", platform)
            }
            return
        }

        mrecAdView = MaxAdView(placementName, activity)

        val widthPx = AppLovinSdkUtils.dpToPx(activity, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(activity, 250)
        mrecAdView?.layoutParams = RelativeLayout.LayoutParams(widthPx, heightPx).apply {
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        }


        mrecAdView.setListener(object : MaxAdViewAdListener {
            override fun onAdLoaded(ad: MaxAd?) {
                viewIntances[placementName] = mrecAdView
            }

            override fun onAdDisplayed(ad: MaxAd?) {
                listener?.onDisplayed(platform)
            }

            override fun onAdHidden(ad: MaxAd?) {

            }

            override fun onAdClicked(ad: MaxAd?) {
                listener?.onClicked(platform)
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
                viewIntances[placementName] = null
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView, containerView)
                }

                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> error code=${error?.code} / ${error?.message}", platform)
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {

            }

            override fun onAdExpanded(ad: MaxAd?) {

            }

            override fun onAdCollapsed(ad: MaxAd?) {

            }

        })
        _removeBannerViewIfExists(mrecAdView, containerView)
        containerView.addView(mrecAdView)

        mrecAdView.loadAd()
    }

    override fun hasLoadedNative(nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): Boolean {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }

        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
        return nativeAds.size > 0
    }

    override fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }

        var nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()

        try {
            nativeAds.forEach {
                (it as MaxNativeAdView).recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            nativeAds.clear()

            viewIntances.put(placementName, nativeAds)
        }

        val nativeAdLoader = MaxNativeAdLoader(placementName, activity)
        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd?) {
                super.onNativeAdLoaded(nativeAdView, ad)

                nativeAdView?.let {
                    nativeAds.add(it)
                    viewIntances.put(placementName, nativeAds)
                }

                listener?.onLoaded(platform)
            }

            override fun onNativeAdLoadFailed(adUnitId: String?, error: MaxError?) {
                super.onNativeAdLoadFailed(adUnitId, error)

                listener?.onError(AdErrorMode.PLATFORM, error?.message ?: "applovin native ad load error", platform)
            }

            override fun onNativeAdClicked(ad: MaxAd?) {
                super.onNativeAdClicked(ad)
            }
        })
        nativeAdLoader.loadAd()

    }


    private var lastLoadedNativeAdPositions: MutableMap<String, Int> = mutableMapOf()

    override fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener?, placementGroupIndex: Int): Boolean {

        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }

        val nativeAds: ArrayList<Any> = if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()

        val lastLoadedNativeAdPosition: Int = if (lastLoadedNativeAdPositions.containsKey(placementName)) lastLoadedNativeAdPositions[placementName]!! else -1
        var showPositionAt = if (lastLoadedNativeAdPosition == -1) 0 else lastLoadedNativeAdPosition + 1

        if (showPositionAt >= nativeAds.size) {
            showPositionAt = 0
        }

        if (nativeAds.size == 0) {
            return false
        }
        lastLoadedNativeAdPositions[placementName] = showPositionAt

        val nativeAd = nativeAds[showPositionAt] as MaxNativeAdView?

        nativeAd?.let {
            _removeViewIfExists(nativeAd, containerView)
            // containerView.removeAllViews()
            containerView.addView(nativeAd)

        }
        return nativeAd != null
    }

    override fun getNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): ArrayList<Any> {
        val placementName = if (nativeAdFormat == AdFormatEnum.NATIVE) {
            getPlacementGroupByIndex(placementGroupIndex).native
        } else {
            getPlacementGroupByIndex(placementGroupIndex).nativeMedium
        }

        return if (viewIntances.containsKey(placementName) && viewIntances[placementName] != null) viewIntances.get(placementName) as ArrayList<Any> else ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]

            var bannerAdView: MaxAdView? = if (viewIntances.containsKey(pg.banner)) viewIntances.get(pg.banner) as MaxAdView? else null
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

            var mrecAdView: MaxAdView? = if (viewIntances.containsKey(pg.mrec)) viewIntances.get(pg.mrec) as MaxAdView? else null
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