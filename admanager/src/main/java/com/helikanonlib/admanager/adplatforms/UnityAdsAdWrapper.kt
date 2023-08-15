package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize


class UnityAdsAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    companion object {
        var isInitialized = false
    }

    override var platform = AdPlatformTypeEnum.UNITYADS
    var viewIntances: MutableMap<String, Any?> = mutableMapOf()


    override fun initialize(activity: Activity, testMode: Boolean) {
    }

    override fun initialize(context: Context, testMode: Boolean) {
        if (isInitialized) return

        /*UnityAds.addListener(object : IUnityAdsListener {
            override fun onUnityAdsReady(placementId: String?) {

            }

            override fun onUnityAdsStart(placementId: String?) {

            }

            override fun onUnityAdsFinish(placementId: String?, result: UnityAds.FinishState?) {

            }

            override fun onUnityAdsError(error: UnityAds.UnityAdsError?, message: String?) {

            }
        })*/
        UnityAds.initialize(context, appId, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {

            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {

            }

        })

        isInitialized = true
    }

    override fun enableTestMode(context: Context, deviceId: String?) {
        UnityAds.initialize(context, appId, true, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {

            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {

            }

        })
        /*for (group in placementGroups) {
            group.interstitial = "ca-app-pub-3940256099942544/1033173712"
            group.banner = "ca-app-pub-3940256099942544/6300978111"
            group.rewarded = "ca-app-pub-3940256099942544/5224354917"
            group.mrec = "ca-app-pub-3940256099942544/6300978111"
            group.native = "ca-app-pub-3940256099942544/2247696110"
            group.appOpenAd = "ca-app-pub-3940256099942544/3419835294"
        }*/


    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }
        /*else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> unityads interstitial load error", platform)
            return
        }*/


        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        viewIntances.put(placementName, null)

        UnityAds.load(placementName, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                viewIntances.put(placementName, placementId)
                updateLastLoadInterstitialDateByAdPlatform(platform)

                listener?.onLoaded(platform)
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${error?.name} / ${message}", platform)
            }

        })


    }

    override fun showInterstitial(activity: Activity, shownWhere: String, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial [$placementId] >> ${error?.name ?: ""}", platform)
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                listener?.onDisplayed(platform)
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                listener?.onClicked(platform)
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

        })
        viewIntances[placementName] = null

    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        // return viewIntances.containsKey(placementName) && viewIntances[placementName] != null

        var isLoaded = viewIntances.containsKey(placementName) && viewIntances[placementName] != null
        if (isLoaded && !isValidLoadedInterstitial(platform)) {
            viewIntances.put(placementName, null)
            isLoaded = false
        }

        return isLoaded
    }

    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null

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
        var bannerAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null

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


        bannerAdView = BannerView(activity, placementName, UnityBannerSize(320, 50))
        bannerAdView.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerAdView: BannerView?) {
                activity.runOnUiThread {
                    viewIntances[placementName] = bannerAdView

                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onBannerShown(bannerAdView: BannerView?) {

            }

            override fun onBannerClick(bannerAdView: BannerView?) {
                listener?.onClicked(platform)
            }

            override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=${errorInfo?.errorCode ?: ""} / ${errorInfo?.errorMessage ?: ""}", platform)
            }

            override fun onBannerLeftApplication(bannerView: BannerView?) {

            }

        }
        bannerAdView.load()

    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        /*else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> unityads rewarded load error", platform)
            return
        }*/

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        viewIntances.put(placementName, null)

        UnityAds.load(placementName, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                viewIntances.put(placementName, placementId)
                updateLastLoadRewardedDateByAdPlatform(platform)

                listener?.onLoaded(platform)
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> error code=${error?.name} / ${message}", platform)
            }

        })
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded [$placementId] >> ${error?.name ?: ""}", platform)
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                listener?.onDisplayed(platform)
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                listener?.onClicked(platform)
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                viewIntances[placementName] = null
                listener?.onClosed(platform)
            }

        })
    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded

        var isLoaded = viewIntances.containsKey(placementName) && viewIntances[placementName] != null
        if (isLoaded && !isValidLoadedRewarded(platform)) {
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val mrecAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null

        var isLoaded = _isBannerLoaded(mrecAdView)
        if (isLoaded && !isValidLoadedBanner(platform)) {
            _removeBannerViewIfExists(mrecAdView)
            viewIntances[placementName] = null
            isLoaded = false
        }

        return isLoaded
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> unityads mrec not supported", platform)


        /*val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        var bannerAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null

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
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> ismrecloaded", platform)
            }
            return
        }


        bannerAdView = BannerView(activity, placementName, UnityBannerSize(300, 250))
        bannerAdView.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerAdView: BannerView?) {
                activity.runOnUiThread {
                    viewIntances[placementName] = bannerAdView

                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onBannerClick(bannerAdView: BannerView?) {
                listener?.onClicked(platform)
            }

            override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> error code=${errorInfo?.errorCode ?: ""} / ${errorInfo?.errorMessage ?: ""}", platform)
            }

            override fun onBannerLeftApplication(bannerView: BannerView?) {

            }

        }
        bannerAdView.load()*/

    }

    override fun hasLoadedNative(nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): Boolean {
        return false
    }

    override fun loadNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        listener?.onError(AdErrorMode.PLATFORM, "unity ads not support native ads", platform)
    }

    override fun showNative(activity: Activity, nativeAdFormat: AdFormatEnum, containerView: ViewGroup, listener: AdPlatformShowListener?, placementGroupIndex: Int): Boolean {
        return false
    }

    override fun getNativeAds(activity: Activity, nativeAdFormat: AdFormatEnum, placementGroupIndex: Int): ArrayList<Any> {
        return ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {
        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]
                viewIntances.put(pg.interstitial, null)
                viewIntances.put(pg.rewarded, null)
            }

            destroyBanner(activity)
            destroyMrec(activity)
        } catch (e: Exception) {
            Log.e("UnitAds", e.message ?: "")
        }
    }

    override fun destroyBanner(activity: Activity) {
        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]

                var bannerAdView: BannerView? = if (viewIntances.containsKey(pg.banner)) viewIntances.get(pg.banner) as BannerView? else null
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
        } catch (e: Exception) {
            Log.e("UnitAds", e.message ?: "")
        }


    }

    override fun destroyMrec(activity: Activity) {
        try {
            for (i in 0 until placementGroups.size) {
                val pg = placementGroups[i]

                var mrecAdView: BannerView? = if (viewIntances.containsKey(pg.mrec)) viewIntances.get(pg.mrec) as BannerView? else null
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

        } catch (e: Exception) {
            Log.e("UnitAds", e.message ?: "")
        }
    }


}
