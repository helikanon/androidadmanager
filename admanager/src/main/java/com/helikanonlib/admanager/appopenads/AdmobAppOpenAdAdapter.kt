package com.helikanonlib.admanager

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

internal class AdmobAppOpenAdAdapter(
    private val application: Application,
    private val placementId: String
) : AppOpenAdAdapter {
    override val platform = AdPlatformTypeEnum.ADMOB

    private var appOpenAd: AppOpenAd? = null
    private var loadElapsedRealtime = 0L

    override fun isReady(validityDurationMillis: Long, nowElapsedRealtime: Long): Boolean {
        return appOpenAd != null && AppOpenAdPolicy.wasLoadedRecently(
            loadElapsedRealtime,
            validityDurationMillis,
            nowElapsedRealtime
        )
    }

    override fun load(callback: AppOpenAdLoadCallback) {
        try {
            AppOpenAd.load(
                application,
                placementId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        loadElapsedRealtime = SystemClock.elapsedRealtime()
                        callback.onLoaded()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        invalidate()
                        callback.onError(error.message)
                    }
                }
            )
        } catch (error: Exception) {
            invalidate()
            callback.onError(error.message.orEmpty())
        }
    }

    override fun show(activity: Activity, callback: AppOpenAdDisplayCallback): Boolean {
        val ad = appOpenAd ?: run {
            callback.onError("ADMOB app open ad is not ready")
            return false
        }
        return try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    invalidate()
                    callback.onError(error.message)
                }

                override fun onAdShowedFullScreenContent() {
                    callback.onDisplayed()
                }

                override fun onAdDismissedFullScreenContent() {
                    invalidate()
                    callback.onClosed()
                }
            }
            ad.show(activity)
            true
        } catch (error: Exception) {
            invalidate()
            callback.onError(error.message.orEmpty())
            false
        }
    }

    override fun invalidate() {
        appOpenAd = null
        loadElapsedRealtime = 0L
    }
}
