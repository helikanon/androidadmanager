package com.helikanonlib.admanager.adplatforms

import admost.sdk.AdMostInterstitial
import admost.sdk.AdMostView
import admost.sdk.base.AdMostConfiguration
import admost.sdk.listener.AdMostBannerCallBack
import admost.sdk.listener.AdMostFullScreenCallBack
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.helikanonlib.admanager.*

class AdmostAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {

    override var platform = AdPlatformTypeEnum.ADMOST
    var viewIntances: MutableMap<String, Any?> = mutableMapOf()


    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity, testMode: Boolean) {
        if (isInitialized) return

        val configuration = AdMostConfiguration.Builder(activity, appId)
        configuration.setSubjectToGDPR(false)
        configuration.setUserConsent(true)
        configuration.setSubjectToCCPA(false)
        configuration.showUIWarningsForDebuggableBuild(BuildConfig.DEBUG)

        isInitialized = true

    }

    override fun initialize(context: Context, testMode: Boolean) {

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

        var interstitial: AdMostInterstitial? = null

        interstitial = AdMostInterstitial(activity, placementName, object : AdMostFullScreenCallBack() {
            override fun onReady(network: String?, ecpm: Int) {
                super.onReady(network, ecpm)
                listener?.onLoaded(platform)

                updateLastLoadInterstitialDateByAdPlatform(platform)
            }

            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)

                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> error code=${errorCode} / ${errorCode}", platform)
            }
        })
        interstitial.refreshAd(false)
        viewIntances.put(placementName, interstitial)

    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isInterstitialLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: AdMostInterstitial? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostInterstitial? else null

        interstitial?.setListener(object : AdMostFullScreenCallBack() {
            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)

                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial show >> error code=${errorCode} / ${errorCode}", platform)
            }

            override fun onDismiss(message: String?) {
                super.onDismiss(message)

                viewIntances[placementName] = null
                listener?.onClosed(platform)

                interstitial?.destroy()
                try {

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onShown(network: String?) {
                super.onShown(network)
                listener?.onDisplayed(platform)
            }

            override fun onClicked(network: String?) {
                super.onClicked(network)
                listener?.onClicked(platform)
            }

        })
        interstitial?.show()
        viewIntances[placementName] = null // gösterir göstermez boşalt

    }

    override fun isInterstitialLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).interstitial
        val interstitial: AdMostInterstitial? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostInterstitial? else null

        var isLoaded = interstitial != null && interstitial.isLoaded && interstitial.isReadyToShow
        if (isLoaded && !isValidLoadedInterstitial(platform)) {
            viewIntances.put(placementName, null)
            isLoaded = false
        }

        return isLoaded
    }

    override fun isBannerLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).banner
        val admostView: AdMostView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostView? else null
        val bannerAdView = admostView?.view as ViewGroup?

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
        var admostView: AdMostView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostView? else null
        val bannerAdView = admostView?.view as ViewGroup?

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


        admostView = AdMostView(activity, placementName, object : AdMostBannerCallBack() {
            override fun onReady(network: String?, ecpm: Int, adView: View?) {
                super.onReady(network, ecpm, adView)

                activity.runOnUiThread {

                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> error code=${errorCode} / ${errorCode}", platform)
            }

            override fun onClick(network: String?) {
                super.onClick(network)
                listener?.onClicked(platform)
            }

            override fun onAdRevenuePaid(impressionData: AdMostFullScreenCallBack.AdMostImpressionData?) {
                super.onAdRevenuePaid(impressionData)
            }

        }, null)
        admostView.load()
        viewIntances[placementName] = admostView
    }

    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {
        if (isRewardedLoaded(placementGroupIndex)) {
            listener?.onLoaded(platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        viewIntances.put(placementName, null)

        var rewarded: AdMostInterstitial? = null

        rewarded = AdMostInterstitial(activity, placementName, object : AdMostFullScreenCallBack() {
            override fun onReady(network: String?, ecpm: Int) {
                super.onReady(network, ecpm)
                listener?.onLoaded(platform)

                updateLastLoadRewardedDateByAdPlatform(platform)
            }

            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)

                viewIntances.put(placementName, null)
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> error code=${errorCode} / ${errorCode}", platform)
            }
        })
        rewarded.refreshAd(false)
        viewIntances.put(placementName, rewarded)

    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?, placementGroupIndex: Int) {
        if (!isRewardedLoaded(placementGroupIndex)) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noads loaded", platform)
            return
        }

        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewarded: AdMostInterstitial? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostInterstitial? else null

        rewarded?.setListener(object : AdMostFullScreenCallBack() {
            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)

                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded show >> error code=${errorCode} / ${errorCode}", platform)
            }

            override fun onDismiss(message: String?) {
                super.onDismiss(message)

                viewIntances[placementName] = null
                listener?.onClosed(platform)

                rewarded?.destroy()
            }

            override fun onShown(network: String?) {
                super.onShown(network)
                listener?.onDisplayed(platform)
            }

            override fun onClicked(network: String?) {
                super.onClicked(network)
            }

            override fun onComplete(network: String?) {
                super.onComplete(network)

                listener?.onRewarded("reward", 1, platform)
            }

        })
        rewarded?.show()
        viewIntances[placementName] = null // gösterir göstermez boşalt

    }

    override fun isRewardedLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).rewarded
        val rewarded: AdMostInterstitial? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostInterstitial? else null

        var isLoaded = rewarded != null && rewarded.isLoaded && rewarded.isReadyToShow
        if (isLoaded && !isValidLoadedRewarded(platform)) {
            viewIntances.put(placementName, null)
            isLoaded = false
        }

        return isLoaded
    }

    override fun isMrecLoaded(placementGroupIndex: Int): Boolean {
        val placementName = getPlacementGroupByIndex(placementGroupIndex).mrec
        val admostView: AdMostView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostView? else null
        val mrecAdView = admostView?.view as ViewGroup?

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
        var admostView: AdMostView? = if (viewIntances.containsKey(placementName)) viewIntances.get(placementName) as AdMostView? else null
        val mrecAdView = admostView?.view as ViewGroup?

        val lp = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            .apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
            }

        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView, lp)
                listener?.onDisplayed(platform)
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> ismrecloaded", platform)
            }
            return
        }


        admostView = AdMostView(activity, placementName, object : AdMostBannerCallBack() {
            override fun onReady(network: String?, ecpm: Int, adView: View?) {
                super.onReady(network, ecpm, adView)

                activity.runOnUiThread {

                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView, lp)
                    listener?.onDisplayed(platform)
                }
            }

            override fun onFail(errorCode: Int) {
                super.onFail(errorCode)
                viewIntances[placementName] = null
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> error code=${errorCode} / ${errorCode}", platform)
            }

            override fun onClick(network: String?) {
                super.onClick(network)
                listener?.onClicked(platform)
            }

        }, null)
        admostView.load()
        viewIntances[placementName] = admostView
    }

    override fun isNativeLoaded(placementGroupIndex: Int): Boolean {
        return false
    }

    override fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener?, placementGroupIndex: Int) {

    }

    override fun showNative(activity: Activity, pos: Int, listener: AdPlatformShowListener?, placementGroupIndex: Int): NativeAd? {
        return null
    }

    override fun getNativeAds(activity: Activity, placementGroupIndex: Int): ArrayList<Any> {
        return ArrayList<Any>()
    }

    override fun destroy(activity: Activity) {
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]

            var admostView: AdMostView? = if (viewIntances.containsKey(pg.banner)) viewIntances.get(pg.banner) as AdMostView? else null
            var bannerAdView = admostView?.view as ViewGroup?

            admostView?.let {
                if (_isBannerLoaded(bannerAdView)) {
                    try {
                        _removeBannerViewIfExists(bannerAdView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            admostView?.destroy()
            admostView = null
            viewIntances[pg.banner] = null
        }
    }

    override fun destroyMrec(activity: Activity) {
        for (i in 0 until placementGroups.size) {
            val pg = placementGroups[i]

            var admostView: AdMostView? = if (viewIntances.containsKey(pg.mrec)) viewIntances.get(pg.mrec) as AdMostView? else null
            var bannerAdView = admostView?.view as ViewGroup?

            admostView?.let {
                if (_isBannerLoaded(bannerAdView)) {
                    try {
                        _removeBannerViewIfExists(bannerAdView)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }


            admostView?.destroy()
            admostView = null
            viewIntances[pg.mrec] = null
        }
    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}