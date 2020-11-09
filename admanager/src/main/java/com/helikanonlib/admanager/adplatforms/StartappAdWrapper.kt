package com.helikanonlib.admanager.adplatforms


import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.helikanonlib.admanager.*
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.ads.banner.BannerListener
import com.startapp.sdk.ads.banner.Mrec
import com.startapp.sdk.ads.nativead.NativeAdPreferences
import com.startapp.sdk.ads.nativead.StartAppNativeAd
import com.startapp.sdk.adsbase.Ad
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

    var startAppAd: StartAppAd? = null
    var bannerAdView: Banner? = null
    var startAppAdRewarded: StartAppAd? = null
    var mrecAdView: Mrec? = null


    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
        if (isInitialized) return

        StartAppSDK.init(activity.applicationContext, appId, false);
        StartAppAd.disableSplash();
        StartAppAd.disableAutoInterstitial()

        /*StartAppSDK.setUserConsent(
            context,
            "pas",
            System.currentTimeMillis(),
            true
        );*/

        startAppAd = StartAppAd(activity.applicationContext)
        startAppAdRewarded = StartAppAd(activity.applicationContext)

        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        StartAppSDK.setTestAdsEnabled(true);
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        startAppAd?.loadAd(object : AdEventListener {
            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onLoaded(platform)
            }
        })
    }

    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) return

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

    override fun isInterstitialLoaded(): Boolean {
        return startAppAd?.isReady ?: false
    }

    override fun isBannerLoaded(): Boolean {
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {

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
    }


    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded(platform)
            return
        }

        startAppAdRewarded?.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onFailedToReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p0?.errorMessage ?: ""}", platform)
            }

            override fun onReceiveAd(p0: com.startapp.sdk.adsbase.Ad?) {
                listener?.onLoaded(platform)
            }
        })
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noadsloaded", platform)
            return
        }

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

    override fun isRewardedLoaded(): Boolean {
        return startAppAdRewarded?.isReady ?: false
    }

    override fun isMrecLoaded(): Boolean {
        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
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
    }

    override fun isNativeLoaded(): Boolean {
        return nativeAds.size>0
    }

    override fun loadNativeAds(activity: Activity, count: Int,listener: AdPlatformLoadListener?) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun showNative(activity: Activity, pos: Int, containerView: ViewGroup, adSize: String, listener: AdPlatformShowListener?) {
        listener?.onError(AdErrorMode.PLATFORM, "not supported native ad >> ${platform.name}", platform)
    }

    override fun destroy(activity: Activity) {
        startAppAd = null
        startAppAdRewarded = null
        destroyBanner(activity)
        destroyMrec(activity)
    }

    override fun destroyBanner(activity: Activity) {
        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bannerAdView = null
    }

    override fun destroyMrec(activity: Activity) {
        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mrecAdView = null
    }

    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}

}

