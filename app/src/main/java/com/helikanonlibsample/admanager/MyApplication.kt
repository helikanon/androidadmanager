package com.helikanonlibsample.admanager

import android.app.Application
import android.util.Log
// import androidx.multidex.MultiDexApplication
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.adplatforms.*

class MyApplication : Application() {

    private lateinit var appOpenLifecycleController: AppOpenLifecycleController


    companion object {
        lateinit var adManager: AdManager
        var appOpenAdManager: AppOpenAdManager? = null
    }


    override fun onCreate() {
        super.onCreate()

        initAdManager()

        appOpenAdManager = AppOpenAdManager(
            this,
            mutableMapOf(
                AdPlatformTypeEnum.ADMOB to "ca-app-pub-3940256099942544/9257395921",
                AdPlatformTypeEnum.APPLOVIN to "dd9249369deec4ec",
            ),
            "applovin,admob",

            object : AdPlatformShowListener() {
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum) {
                    Log.e("adManager", "AppOpenAdManager >>> success display")
                }

                override fun onPlatformError(error: AdPlatformError) {
                    Log.e("adManager", "AppOpenAdManager platform show error >>> ${error.message}")
                }

                override fun onError(error: AdManagerError) {
                    Log.e("adManager", "AppOpenAdManager show error >>> ${error.message}")
                }

            },
            object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
                    Log.e("adManager", "AppOpenAdManager >>> success load")
                }

                override fun onPlatformError(error: AdPlatformError) {
                    Log.e("adManager", "AppOpenAdManager platform load error >>> ${error.message}")
                }

                override fun onError(error: AdManagerError) {
                    Log.e("adManager", "AppOpenAdManager load error >>> ${error.message}")
                }

            }
        )

        appOpenAdManager?.excludedActivities?.add(JavaSampleActivity::class.java.simpleName)
        appOpenAdManager?.minElapsedSecondsToNextShow = 10

        appOpenLifecycleController = AppOpenLifecycleController(this) {
            appOpenAdManager
        }.also { it.register() }

        adManager.initializePlatforms(
            applicationContext,
            onAllInitializeComplete = {
                appOpenAdManager?.load()
            },
            onPlatformInitializeComplete = {}
        )
    }

    fun initAdManager() {
        adManager = AdManager().apply {
            showAds = true
            autoLoadForInterstitial = true
            isEnabledLoadAndShowIfNotExistsAdsOnAutoloadMode = true
            autoLoadDelay = 11 // seconds

            autoLoadForRewarded = true

            randomInterval = 30 // random seconds for showing interstitial. Interstitial will show after previous showing passed seconds between 60-90
            interstitialMinElapsedSecondsToNextShow = 30
            rewardedMinElapsedSecondsToNextShow = 30


            testMode = BuildConfig.DEBUG
            deviceId = "47088e48-5195-4757-90b2-0da94116befd" // necessary if testmode enabled
            placementGroups = arrayListOf("default")
            adPlatforms = mutableListOf<AdPlatformModel>(
                /*AdPlatformModel(
                    FacebookAdWrapper("your_app_id").apply {
                        interstitialPlacementId = "YOUR_PLACEMENT_ID"
                        bannerPlacementId = "YOUR_PLACEMENT_ID"
                        rewardedPlacementId = "YOUR_PLACEMENT_ID"
                        mrecPlacementId = "YOUR_PLACEMENT_ID"
                    },
                    true, true, true, true
                ),*/
                AdPlatformModel(
                    AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713").apply {
                        placementGroups.add(
                            AdPlacementGroupModel(
                                groupName = "default",
                                interstitial = "ca-app-pub-3940256099942544/1033173712",
                                rewarded = "ca-app-pub-3940256099942544/5224354917",
                                banner = "ca-app-pub-3940256099942544/6300978111",
                                mrec = "ca-app-pub-3940256099942544/9214589741",
                                native = "ca-app-pub-3940256099942544/2247696110",
                                appOpenAd = "ca-app-pub-3940256099942544/9257395921",
                                nativeMedium = "ca-app-pub-3940256099942544/1044960115"
                            )
                        )
                        /*placementGroups.add(
                            AdPlacementGroupModel(
                                groupName = "second_group",
                                interstitial = "ca-app-pub-3940256099942544/8691691433",
                                rewarded = "ca-app-pub-3940256099942544/5354046379",
                                banner = "ca-app-pub-3940256099942544/6300978111",
                                mrec = "ca-app-pub-3940256099942544/6300978111",
                                native = "ca-app-pub-3940256099942544/1044960115",
                                appOpenAd = "ca-app-pub-3940256099942544/3419835294"
                            )
                        )*/
                    },
                    true, true, true, true
                ),
                AdPlatformModel(
                    UnityAdsAdWrapper("4428087").apply {
                        placementGroups.add(
                            AdPlacementGroupModel(
                                groupName = "default",
                                interstitial = "Interstitial_Android",
                                rewarded = "Rewarded_Android",
                                banner = "Banner_Android",
                                mrec = "BannerMrec_Android",
                                native = "DefaultNative",
                                appOpenAd = "",
                                nativeMedium = ""
                            )
                        )
                    },
                    true, true, true, true
                ),
                /*AdPlatformModel(
                    AdmostAdWrapper("6cc8e89a-b52a-4e9a-bb8c-579f7ec538fe").apply {
                        placementGroups.add(
                            AdPlacementGroupModel(
                                groupName = "default",
                                interstitial = "f99e409b-f9ab-4a2e-aa9a-4d143e6809ae",
                                rewarded = "88cfcfd0-2f8c-4aba-9f36-cc0ac99ab140",
                                banner = "86644357-21d0-45a4-906a-37262461df65",
                                mrec = "86644357-21d0-45a4-906a-37262461df65",
                                native = "",
                                appOpenAd = ""
                            )
                        )
                    },
                    true, true, true, true
                ),*/
                AdPlatformModel(
                    ApplovinAdWrapper("pHS9IvJQm4f3s9fPw3xpJLFIu6lLDfukm72lv_OqABUrmgxiCnDKCx4vAPNHI9lOv3oDcJZngd-ek0cHus5pBP").apply {
                        placementGroups.add(
                            AdPlacementGroupModel(
                                groupName = "default",
                                interstitial = "7c4a01242eaee289",
                                rewarded = "f2f5534658a6b4ab",
                                banner = "609a039e1d803bea",
                                mrec = "851a6927fdae17d5",
                                native = "2b1686bf9db060d3",
                                appOpenAd = "dd9249369deec4ec",
                                nativeMedium = "96454048eeffaad2"
                            )
                        )
                    },
                    true, true, true, true
                )
            )
        }


        /*adManager.addAdPlatform(
            AdPlatformModel(
                MopubAdWrapper("207754325").apply {
                    interstitialPlacementId = "b75290feb5a74c79b2e7bf027a02f268"
                    bannerPlacementId = "1ac121edbb324cbf989df50731f69eae"
                    rewardedPlacementId = "7c13c6edc67a4fcab83e3cb45bd46597"
                    mrecPlacementId = "b311abd32a8944f4b6c6bba7fdb1f9e0"
                },
                true, true, true, true
            )
        )*/


        adManager.setAdPlatformSortByAdFormatStr(0, "interstitial", "applovin,admob")
        adManager.setAdPlatformSortByAdFormatStr(0, "banner", "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(0, "rewarded", "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(0, "mrec", "admob,applovin")
        adManager.setAdPlatformSortByAdFormatStr(0, "native", "applovin")
        adManager.setAdPlatformSortByAdFormatStr(0, "native_medium", "applovin")


        adManager.globalInterstitialLoadListener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[LOAD][INTERSTITIAL][PLATFORM] ${error.message} ${error.platform.name}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[LOAD][INTERSTITIAL][MANAGER] ${error.message}")
            }
        }
        adManager.globalRewardedLoadListener = object : AdPlatformLoadListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[LOAD][REWARDED][PLATFORM] ${error.message} ${error.platform.name}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[LOAD][REWARDED][MANAGER] ${error.message}")
            }
        }

        adManager.globalInterstitialShowListener = object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[SHOW][INTERSTITIAL][PLATFORM] ${error.message} ${error.platform.name}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[SHOW][INTERSTITIAL][MANAGER] ${error.message}")
            }
        }

        adManager.globalRewardedShowListener = object : AdPlatformShowListener() {
            override fun onPlatformError(error: AdPlatformError) {
                Log.d("adManager", "[SHOW][REWARDED][PLATFORM] ${error.message} ${error.platform.name}")
            }

            override fun onError(error: AdManagerError) {
                Log.d("adManager", "[SHOW][REWARDED][MANAGER] ${error.message}")
            }
        }

        // OR
        /*
        adManager.initializePlatforms()

        if (BuildConfig.DEBUG) {
            adManager.enableTestMode("47088e48-5195-4757-90b2-0da94116befd") // send device id, it is necessary for test facebook audience networks ad
        }

        adManager.start()
         */

        // OR
        /*
        adManager.initializePlatforms()
        adManager.loadInterstitial(object:AdPlatformLoadListener(){
            override fun onError(error: AdManagerError) {
            }
        })
        adManager.loadRewarded(object:AdPlatformLoadListener(){
            override fun onError(error: AdManagerError) {
            }
        })
        */


        /*adManager.loadInterstitial(object : AdPlatformLoadListener() {
            override fun onLoaded() {
                super.onLoaded()
            }
        })

        adManager.loadRewarded(object : AdPlatformLoadListener() {
            override fun onLoaded() {
                super.onLoaded()
            }
        })*/
    }

}
