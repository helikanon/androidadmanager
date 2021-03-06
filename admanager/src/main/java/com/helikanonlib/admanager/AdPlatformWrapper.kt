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

    abstract fun destroy(activity: Activity)
    abstract fun destroyBanner(activity: Activity)
    abstract fun destroyMrec(activity: Activity)


    public open fun onCreate(activity: Activity) {}
    public open fun onPause(activity: Activity) {}
    public open fun onStop(activity: Activity) {}
    public open fun onResume(activity: Activity) {}


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


