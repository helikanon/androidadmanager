package com.helikanonlib.admanager

import android.app.Activity
import android.view.ViewGroup
import android.widget.RelativeLayout

abstract class AdPlatformWrapper(open var appId: String) {

    abstract val platform: AdPlatformTypeEnum

    var interstitialPlacementId: String? = null
    var bannerPlacementId: String? = null
    var rewardedPlacementId: String? = null
    var mrecPlacementId: String? = null
    var nativePlacementId: String? = null

    abstract fun initialize(activity: Activity)
    abstract fun enableTestMode(deviceId: String? = null)

    abstract fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null)
    abstract fun showInterstitial(activity: Activity, listener: AdPlatformShowListener? = null)
    abstract fun isInterstitialLoaded(): Boolean

    abstract fun isBannerLoaded(): Boolean
    abstract fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null)

    abstract fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null)
    abstract fun showRewarded(activity: Activity, listener: AdPlatformShowListener? = null)
    abstract fun isRewardedLoaded(): Boolean

    abstract fun isMrecLoaded(): Boolean
    abstract fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null)


    val nativeAds: ArrayList<Any> = arrayListOf()
    abstract fun isNativeLoaded(): Boolean
    abstract fun loadNativeAds(activity: Activity,count: Int, listener: AdPlatformLoadListener? = null)
    // adSize >> [small,medium]
    abstract fun showNative(activity: Activity, pos: Int, containerView: ViewGroup, adSize: String, listener: AdPlatformShowListener? = null)

    abstract fun destroy(activity: Activity)
    abstract fun destroyBanner(activity: Activity)
    abstract fun destroyMrec(activity: Activity)

    open fun onCreate(activity: Activity) {}
    open fun onPause(activity: Activity) {}
    open fun onStop(activity: Activity) {}
    open fun onResume(activity: Activity) {}

    protected fun _isBannerLoaded(bannerAdView: ViewGroup?): Boolean {
        return bannerAdView != null && bannerAdView.parent != null
    }

    protected fun _removeBannerViewIfExists(bannerAdView: ViewGroup?): Boolean {
        if (_isBannerLoaded(bannerAdView)) {
            (bannerAdView?.parent as ViewGroup).removeView(bannerAdView)
            return true
        }

        return false
    }
}


