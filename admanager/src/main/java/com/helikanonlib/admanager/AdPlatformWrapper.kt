package com.helikanonlib.admanager

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout

abstract class AdPlatformWrapper(open var appId: String, open var activity: Activity, open var context: Context) {

    abstract val platform: AdPlatformTypeEnum

    var interstitialPlacementId: String? = null
    var bannerPlacementId: String? = null
    var rewardedPlacementId: String? = null
    var mrecPlacementId: String? = null

    abstract fun initialize()
    abstract fun enableTestMode(deviceId: String? = null)

    abstract fun loadInterstitial(listener: AdPlatformLoadListener? = null)
    abstract fun showInterstitial(listener: AdPlatformShowListener? = null)
    abstract fun isInterstitialLoaded(): Boolean

    abstract fun showBanner(containerView: RelativeLayout, listener: AdPlatformShowListener? = null)

    abstract fun loadRewarded(listener: AdPlatformLoadListener? = null)
    abstract fun showRewarded(listener: AdPlatformShowListener? = null)
    abstract fun isRewardedLoaded(): Boolean

    abstract fun showMrec(containerView: RelativeLayout, listener: AdPlatformShowListener? = null)

    abstract fun destroy()


    public open fun onCreate() {}
    abstract fun onPause()
    abstract fun onStop()
    abstract fun onResume()


    fun isBannerLoaded(bannerAdView: ViewGroup?): Boolean {
        return bannerAdView != null && bannerAdView.parent != null
    }

    fun removeBannerViewIfExists(bannerAdView: ViewGroup?): Boolean {
        if (isBannerLoaded(bannerAdView)) {
            (bannerAdView?.parent as ViewGroup).removeView(bannerAdView)
            return true
        }

        return false
    }
}


