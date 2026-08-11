package com.helikanonlib.admanager

import android.app.Activity
import android.os.SystemClock
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd

internal class ApplovinAppOpenAdAdapter(
    placementId: String
) : AppOpenAdAdapter {
    override val platform = AdPlatformTypeEnum.APPLOVIN

    private val appOpenAd = MaxAppOpenAd(placementId)
    private var loadElapsedRealtime = 0L

    override fun isReady(validityDurationMillis: Long, nowElapsedRealtime: Long): Boolean {
        return appOpenAd.isReady && AppOpenAdPolicy.wasLoadedRecently(
            loadElapsedRealtime,
            validityDurationMillis,
            nowElapsedRealtime
        )
    }

    override fun load(callback: AppOpenAdLoadCallback) {
        appOpenAd.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) {
                loadElapsedRealtime = SystemClock.elapsedRealtime()
                callback.onLoaded()
            }

            override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                invalidate()
                callback.onError(error.message)
            }

            override fun onAdDisplayed(ad: MaxAd) = Unit
            override fun onAdHidden(ad: MaxAd) = Unit
            override fun onAdClicked(ad: MaxAd) = Unit
            override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) = Unit
        })

        try {
            appOpenAd.loadAd()
        } catch (error: Exception) {
            invalidate()
            callback.onError(error.message.orEmpty())
        }
    }

    override fun show(activity: Activity, callback: AppOpenAdDisplayCallback): Boolean {
        if (!appOpenAd.isReady) {
            callback.onError("APPLOVIN app open ad is not ready")
            return false
        }
        return try {
            appOpenAd.setListener(object : MaxAdListener {
                override fun onAdDisplayed(ad: MaxAd) {
                    callback.onDisplayed()
                }

                override fun onAdHidden(ad: MaxAd) {
                    invalidate()
                    callback.onClosed()
                }

                override fun onAdClicked(ad: MaxAd) {
                    callback.onClicked()
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    invalidate()
                    callback.onError(error.message)
                }

                override fun onAdLoaded(ad: MaxAd) = Unit
                override fun onAdLoadFailed(adUnitId: String, error: MaxError) = Unit
            })
            appOpenAd.showAd()
            true
        } catch (error: Exception) {
            invalidate()
            callback.onError(error.message.orEmpty())
            false
        }
    }

    override fun invalidate() {
        loadElapsedRealtime = 0L
    }
}
