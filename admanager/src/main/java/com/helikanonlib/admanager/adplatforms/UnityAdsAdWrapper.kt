package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.helikanonlib.admanager.AdFormatEnum
import com.helikanonlib.admanager.AdPlatformLoadListener
import com.helikanonlib.admanager.AdPlatformShowListener
import com.helikanonlib.admanager.AdPlatformTypeEnum
import com.helikanonlib.admanager.AdPlatformWrapper
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.InitializationConfiguration
import com.unity3d.ads.InitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize


class UnityAdsAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    companion object {
        var isInitializeStarted = false
    }

    override var platform = AdPlatformTypeEnum.UNITYADS
    var viewIntances: MutableMap<String, Any?> = mutableMapOf()
    private var isTestModeEnabled = false


    override fun initialize(activity: Activity, onInitializeComplete: ((Boolean) -> Unit)?, testMode: Boolean) {
    }

    override fun initialize(context: Context, onInitializeComplete: ((Boolean) -> Unit)?, testMode: Boolean) {
        if (isInitializeStarted || isInitialized) return

        isTestModeEnabled = isTestModeEnabled || testMode
        val config = InitializationConfiguration.Builder(appId)
            .withTestMode(isTestModeEnabled)
            .build()

        val listener: InitializationListener = InitializationListener { error ->
            if (error == null) {
                isInitialized = true
                onInitializeComplete?.invoke(true)
            } else {
                onInitializeComplete?.invoke(false)
            }
        }
        UnityAds.initialize(config, listener)

        isInitializeStarted = true
    }

    override fun enableTestMode(context: Context, deviceId: String?) {
        isTestModeEnabled = true
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }
        /*else {
            listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementGroupIndex, "${platform.name} interstitial >> unityads interstitial load error"))
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
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementGroupIndex, "${platform.name} interstitial >> error code=${error?.name} / ${message}"))
            }

        })


    }

    override fun showInterstitial(activity: Activity, shownWhere: String, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementGroupIndex, "${platform.name} interstitial >> noadsloaded"))
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                viewIntances[placementName] = null
                listener?.onPlatformError(platformError(AdFormatEnum.INTERSTITIAL, placementGroupIndex, "${platform.name} interstitial [$placementId] >> ${error?.name ?: ""}"))
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
                listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementGroupIndex, "${platform.name} banner >> isbannerloaded"))
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
                listener?.onPlatformError(platformError(AdFormatEnum.BANNER, placementGroupIndex, "${platform.name} banner >> error code=${errorInfo?.errorCode ?: ""} / ${errorInfo?.errorMessage ?: ""}"))
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
            listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementGroupIndex, "${platform.name} rewarded >> unityads rewarded load error"))
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
                listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementGroupIndex, "${platform.name} rewarded >> error code=${error?.name} / ${message}"))
            }

        })
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementGroupIndex, "${platform.name} rewarded >> noadsloaded"))
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                viewIntances[placementName] = null
                listener?.onPlatformError(platformError(AdFormatEnum.REWARDED, placementGroupIndex, "${platform.name} rewarded [$placementId] >> ${error?.name ?: ""}"))
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
        listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementGroupIndex, "${platform.name} mrec >> unityads mrec not supported"))


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
                listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementGroupIndex, "${platform.name} mrec >> ismrecloaded"))
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
                listener?.onPlatformError(platformError(AdFormatEnum.MREC, placementGroupIndex, "${platform.name} mrec >> error code=${errorInfo?.errorCode ?: ""} / ${errorInfo?.errorMessage ?: ""}"))
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
        listener?.onPlatformError(platformError(nativeAdFormat, placementGroupIndex, "unity ads not support native ads"))
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
