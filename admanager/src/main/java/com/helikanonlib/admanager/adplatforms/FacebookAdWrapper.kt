package com.helikanonlib.admanager.adplatforms


import android.app.Activity
import android.content.Context
import android.widget.RelativeLayout
import com.facebook.ads.*
import com.helikanonlib.admanager.*


/**
 * *************************************************************************************************
 * FACEBOOK ADS HELPER
 * *************************************************************************************************
 */
class FacebookAdWrapper(override var appId: String) : AdPlatformWrapper(appId) {
    override val platform = AdPlatformTypeEnum.FACEBOOK

    var interstitialAd: InterstitialAd? = null
    var bannerAdView: AdView? = null
    var rewardedVideoAd: RewardedVideoAd? = null
    var mrecAdView: AdView? = null


    companion object {
        var isInitialized = false
    }

    override fun initialize(activity: Activity) {
        if (isInitialized) return
        AudienceNetworkAds.initialize(activity.applicationContext);

        isInitialized = true
    }

    override fun enableTestMode(deviceId: String?) {
        deviceId ?: return
        AdSettings.addTestDevice(deviceId)
    }

    override fun loadInterstitial(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isInterstitialLoaded()) {
            listener?.onLoaded()
            return
        }

        interstitialAd = InterstitialAd(activity.applicationContext, interstitialPlacementId)
        interstitialAd?.setAdListener(object : InterstitialAdListener {
            override fun onInterstitialDisplayed(p0: Ad?) {

            }

            override fun onAdClicked(p0: Ad?) {

            }

            override fun onInterstitialDismissed(p0: Ad?) {

            }

            override fun onError(p0: Ad?, p1: AdError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> ${p1?.errorMessage ?: ""}")
            }

            override fun onAdLoaded(p0: Ad?) {
                listener?.onLoaded()
            }

            override fun onLoggingImpression(p0: Ad?) {

            }

        })
        interstitialAd?.loadAd()
    }


    override fun showInterstitial(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isInterstitialLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} interstitial >> noads loaded")
            return
        }

        if (listener != null) {
            interstitialAd?.setAdListener(object : InterstitialAdListener {
                override fun onInterstitialDisplayed(p0: Ad?) {
                    listener?.onDisplayed()
                }

                override fun onAdClicked(p0: Ad?) {
                    listener?.onClicked()
                }

                override fun onInterstitialDismissed(p0: Ad?) {
                    listener?.onClosed()
                }

                override fun onError(p0: Ad?, p1: AdError?) {
                    listener?.onError(AdErrorMode.PLATFORM, "${platform.name} >> ${p1?.errorMessage ?: ""}")
                }

                override fun onAdLoaded(p0: Ad?) {

                }

                override fun onLoggingImpression(p0: Ad?) {

                }
            })
        }

        interstitialAd?.show()
    }

    override fun isInterstitialLoaded(): Boolean {
        return interstitialAd != null && interstitialAd!!.isAdLoaded && !interstitialAd!!.isAdInvalidated
    }

    override fun isBannerLoaded(): Boolean {
        return _isBannerLoaded(bannerAdView)
    }

    override fun showBanner(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        if (_isBannerLoaded(bannerAdView)) {
            try {
                _removeBannerViewIfExists(bannerAdView)
                containerView.addView(bannerAdView)
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> isbannerloaded error")
            }
            return
        }

        bannerAdView = AdView(activity.applicationContext, bannerPlacementId, AdSize.BANNER_HEIGHT_50)
        bannerAdView?.setAdListener(object : AdListener {

            override fun onError(p0: Ad?, p1: AdError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} banner >> ${p1?.errorMessage ?: ""}")
            }

            override fun onAdLoaded(p0: Ad?) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(bannerAdView)
                    containerView.addView(bannerAdView)
                    listener?.onDisplayed()
                }
            }

            override fun onAdClicked(p0: Ad?) {
                listener?.onClicked()
            }

            override fun onLoggingImpression(p0: Ad?) {}

        })

        bannerAdView?.loadAd()
    }


    override fun loadRewarded(activity: Activity, listener: AdPlatformLoadListener?) {
        if (isRewardedLoaded()) {
            listener?.onLoaded()
            return
        }

        rewardedVideoAd = RewardedVideoAd(activity.applicationContext, rewardedPlacementId)
        rewardedVideoAd?.setAdListener(object : RewardedVideoAdListener {
            override fun onRewardedVideoClosed() {

            }

            override fun onAdClicked(p0: Ad?) {

            }

            override fun onRewardedVideoCompleted() {

            }

            override fun onError(p0: Ad?, p1: AdError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p1?.errorMessage ?: ""}")
            }

            override fun onAdLoaded(p0: Ad?) {
                listener?.onLoaded()
            }

            override fun onLoggingImpression(p0: Ad?) {
                // on video start
            }

        })
        rewardedVideoAd?.loadAd()
    }

    override fun showRewarded(activity: Activity, listener: AdPlatformShowListener?) {
        if (!isRewardedLoaded()) {
            listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> noads loaded")
            return
        }

        rewardedVideoAd?.setAdListener(object : RewardedVideoAdListener {
            override fun onRewardedVideoClosed() {
                listener?.onClosed()
            }

            override fun onAdClicked(p0: Ad?) {
                listener?.onClicked()
            }

            override fun onRewardedVideoCompleted() {
                listener?.onRewarded(null, null)
            }

            override fun onError(p0: Ad?, p1: AdError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} rewarded >> ${p1?.errorMessage ?: ""}")
            }

            override fun onAdLoaded(p0: Ad?) {

            }

            override fun onLoggingImpression(p0: Ad?) {
                // on video start
            }

        })
        rewardedVideoAd?.show()
    }


    override fun isRewardedLoaded(): Boolean {
        return rewardedVideoAd != null && rewardedVideoAd!!.isAdLoaded && !rewardedVideoAd!!.isAdInvalidated
    }


    override fun isMrecLoaded(): Boolean {
        return _isBannerLoaded(mrecAdView)
    }

    override fun showMrec(activity: Activity, containerView: RelativeLayout, listener: AdPlatformShowListener?) {
        if (_isBannerLoaded(mrecAdView)) {
            try {
                _removeBannerViewIfExists(mrecAdView)
                containerView.addView(mrecAdView)
                listener?.onDisplayed()
            } catch (e: Exception) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> isbannerloaded error")
            }
            return
        }

        mrecAdView = AdView(activity.applicationContext, mrecPlacementId, AdSize.RECTANGLE_HEIGHT_250)
        mrecAdView?.setAdListener(object : AdListener {

            override fun onError(p0: Ad?, p1: AdError?) {
                listener?.onError(AdErrorMode.PLATFORM, "${platform.name} mrec >> ${p1?.errorMessage ?: ""}")
            }

            override fun onAdLoaded(p0: Ad?) {
                activity.runOnUiThread {
                    _removeBannerViewIfExists(mrecAdView)
                    containerView.addView(mrecAdView)
                    listener?.onDisplayed()
                }
            }

            override fun onAdClicked(p0: Ad?) {
                listener?.onClicked()
            }

            override fun onLoggingImpression(p0: Ad?) {}

        })

        mrecAdView?.loadAd()
    }


    override fun destroy(activity: Activity) {
        interstitialAd?.destroy()
        interstitialAd = null

        destroyBanner(activity)

        rewardedVideoAd?.destroy()
        rewardedVideoAd = null


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

        bannerAdView?.destroy()
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

        mrecAdView?.destroy()
        mrecAdView = null
    }


    override fun onPause(activity: Activity) {}
    override fun onStop(activity: Activity) {}
    override fun onResume(activity: Activity) {}
}

