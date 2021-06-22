package com.helikanonlib.admanager

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.ads.nativead.NativeAd

abstract class AdPlatformWrapper(open var appId: String) {

    abstract val platform: AdPlatformTypeEnum
    var placementGroups: ArrayList<AdPlacementGroupModel> = ArrayList()
    protected fun getPlacementGroupByIndex(placementGroupIndex: Int): AdPlacementGroupModel {
        /*if (index >= placementGroups.size) {
            return null
        }*/
        return placementGroups.get(placementGroupIndex)
    }

    /*var interstitialPlacementId: String? = null
    var bannerPlacementId: String? = null
    var rewardedPlacementId: String? = null
    var mrecPlacementId: String? = null
    var nativePlacementId: String? = null
    var appOpenAdPlacementId: String? = null*/

    abstract fun initialize(activity: Activity)
    abstract fun initialize(context: Context)
    abstract fun enableTestMode(deviceId: String? = null)

    abstract fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0)
    abstract fun showInterstitial(activity: Activity, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0)
    abstract fun isInterstitialLoaded(placementGroupIndex: Int = 0): Boolean

    abstract fun isBannerLoaded(placementGroupIndex: Int = 0): Boolean
    abstract fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0)

    abstract fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0)
    abstract fun showRewarded(activity: Activity, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0)
    abstract fun isRewardedLoaded(placementGroupIndex: Int = 0): Boolean

    abstract fun isMrecLoaded(placementGroupIndex: Int = 0): Boolean
    abstract fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0)


    // val nativeAds: ArrayList<Any> = arrayListOf()
    abstract fun isNativeLoaded(placementGroupIndex: Int = 0): Boolean
    abstract fun loadNativeAds(activity: Activity, count: Int, listener: AdPlatformLoadListener? = null, placementGroupIndex: Int = 0)

    // adSize >> [small,medium]
    abstract fun showNative(activity: Activity, pos: Int, listener: AdPlatformShowListener? = null, placementGroupIndex: Int = 0): NativeAd?
    abstract fun getNativeAds(activity: Activity, placementGroupIndex: Int = 0): ArrayList<Any>

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


