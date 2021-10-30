package com.helikanonlib.admanager.adplatforms

import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.unity3d.ads.IUnityAdsListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import com.google.android.gms.ads.nativead.NativeAd

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

        UnityAds.addListener(object : IUnityAdsListener {
            override fun onUnityAdsReady(placementId: String?) {

            }

            override fun onUnityAdsStart(placementId: String?) {

            }

            override fun onUnityAdsFinish(placementId: String?, result: UnityAds.FinishState?) {

            }

            override fun onUnityAdsError(error: UnityAds.UnityAdsError?, message: String?) {

            }
        })
        UnityAds.initialize(context, appId, testMode)

        isInitialized = true
    }

    override fun enableTestMode(context: Context, deviceId: String?) {
        UnityAds.initialize(context, appId, true)
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
        } else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> unityads interstitial load error", platform)
            return
        }


    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial [$placementId] >> ${error?.name ?: ""}", platform)
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                listener?.onDisplayed(platform)
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                listener?.onClicked(platform)
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                listener?.onClosed(platform)
            }

        })

    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        return UnityAds.isReady(placementName)
    }

    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val bannerAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null
        return _isBannerLoaded(bannerAdView)
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
        } else {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> unityads rewarded load error", platform)
            return
        }
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        UnityAds.show(activity, placementName, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded [$placementId] >> ${error?.name ?: ""}", platform)
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                listener?.onDisplayed(platform)
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                listener?.onClicked(platform)
            }

            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                listener?.onClosed(platform)
            }

        })
    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        return UnityAds.isReady(placementName)
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val bannerAdView: BannerView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as BannerView? else null
        return _isBannerLoaded(bannerAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
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
        bannerAdView.load()

    }

    override fun isNativeLoaded(placementGroupIndex: Int): Boolean {
        return false
    }

    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        listener?.onError(AdErrorMode.PLATFORM, "unity ads not support native ads", platform)
    }

    override fun showNative(activity: Activity, pos: Int, listener: AdPlatformShowListener?, placementGroupIndex: Int): NativeAd? {
        return null
    }

    override fun getNativeAds(activity: Activity, placementGroupIndex: Int): ArrayList<Any> {
        return ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {

        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            viewIntances.put(pg.interstitial, null)
            viewIntances.put(pg.rewarded, null)
        }

        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {

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


    }

    override fun destroyMrec(activity: Activity) {

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

    }


}
