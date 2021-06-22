package com.helikanonlib.admanager.adplatforms


import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.helikanonlib.admanager.*
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.ads.banner.Mrec
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.VideoListener
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener


/**
 * *************************************************************************************************
 * STARTAPP ADS HELPER
 * *************************************************************************************************
 */
class StartAppAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {
    override val platform = AdPlatformTypeEnum.STARTAPP

    var viewIntances: MutableMap<String, Any?> = mutableMapOf()

    /*var startAppAd: StartAppAd? = null
    var bannerAdView: Banner? = null
    var startAppAdRewarded: StartAppAd? = null
    var mrecAdView: Mrec? = null*/


    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {

    }

    override fun initialize(context: Context) {
        if (isInitialized) return

        StartAppSDK.init(context, appId, false);
        StartAppAd.disableSplash();
        StartAppAd.disableAutoInterstitial()

        /*StartAppSDK.setUserConsent(
            context,
            "pas",
            System.currentTimeMillis(),
            true
        );*/

        //startAppAd = StartAppAd(context)
        // startAppAdRewarded = StartAppAd(context)

        for (group in placementGroups) {
            viewIntances.put("interstitial_${group.groupName}", StartAppAd(context))
            viewIntances.put("rewarded_${group.groupName}", StartAppAd(context))
        }

        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        StartAppSDK.setTestAdsEnabled(true);
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isInterstitialLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = "interstitial_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAd = viewIntances[placementName] as StartAppAd

        startAppAd?.loadAd(object : AdEventListener {
            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onLoaded(platform)
            }
        })
    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) return

        val placementName = "interstitial_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAd = viewIntances[placementName] as StartAppAd


        startAppAd?.showAd(object : AdDisplayListener {
            override fun adHidden(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onClosed(platform)
            }

            override fun adDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onDisplayed(platform)
            }

            override fun adNotDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun adClicked(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onClicked(platform)
            }
        })
    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = "interstitial_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAd = if (viewIntances.containsKey(placementName)) viewIntances[placementName] as StartAppAd? else null
        return startAppAd?.isReady ?: false
    }


    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = "rewarded_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAdRewarded = viewIntances[placementName] as StartAppAd

        startAppAdRewarded?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onLoaded(platform)
            }
        })
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

        val placementName = "rewarded_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAdRewarded = viewIntances[placementName] as StartAppAd

        startAppAdRewarded?.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() {
                listener?.onRewarded(null, null, platform)
            }
        })
        startAppAdRewarded?.showAd(object : AdDisplayListener {
            override fun adHidden(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onClosed(platform)
            }

            override fun adDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onDisplayed(platform)
            }

            override fun adNotDisplayed(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun adClicked(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onClicked(platform)
            }
        })
    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = "rewarded_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        val startAppAdRewarded = if (viewIntances.containsKey(placementName)) viewIntances[placementName] as StartAppAd? else null
        return startAppAdRewarded?.isReady ?: false
    }


    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = "banner_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        var bannerAdView: Banner? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Banner? else null

        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = "banner_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        var bannerAdView: Banner? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Banner? else null


        val lp =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
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

        bannerAdView = Banner(activity, object : BannerListener {
            override fun onClick(p0: View?) {
                listener?.onClicked(platform)
            }

            override fun onFailedToReceiveAd(p0: View?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> failedtoreceivead", platform)
            }

            override fun onReceiveAd(p0: View?) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onImpression(p0: View?) {}

        })
        bannerAdView?.loadAd()
        viewIntances[placementName] = bannerAdView
    }


    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = "mrec_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        var mrecAdView: Mrec? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Mrec? else null

        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        val placementName = "mrec_" + getPlacementGroupByIndex(placementGroupIndex).groupName
        var mrecAdView: Mrec? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Mrec? else null

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

        mrecAdView = Mrec(activity, object : BannerListener {
            override fun onClick(p0: View?) {
                listener?.onClicked(platform)
            }

            override fun onFailedToReceiveAd(p0: View?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> failedtoreceivead", platform)
            }

            override fun onReceiveAd(p0: View?) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onImpression(p0: View?) {}

        })
        mrecAdView?.loadAd()

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

    override fun showNative(activity: Activity, pos: Int, listener: AdPlatformShowListener?, placementGroupIndex: Int): NativeAd? {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
        return null
    }

    override fun getNativeAds(activity: Activity, placementGroupIndex: Int): ArrayList<Any> {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).native
        return ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {
        //startAppAd = null
        // startAppAdRewarded = null
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            val placementName = "banner_" + pg.groupName
            var bannerAdView: Banner? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Banner? else null

            if (_isBannerLoaded(bannerAdView)) {
                try {
                    _removeBannerViewIfExists(bannerAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            bannerAdView = null
        }

    }

    override fun destroyMrec(activity: Activity) {

        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]
            val placementName = "mrec_" + pg.groupName
            var mrecAdView: Mrec? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as Mrec? else null

            if (_isBannerLoaded(mrecAdView)) {
                try {
                    _removeBannerViewIfExists(mrecAdView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mrecAdView = null
        }
    }

    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}

