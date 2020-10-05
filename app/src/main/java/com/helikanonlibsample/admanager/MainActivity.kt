package com.helikanonlibsample.admanager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.adplatforms.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var adManager: AdManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAdManager()
        initViews()
    }

    override fun onResume() {
        super.onResume()

        adManager.onResume(this)

        adManager.showBanner(this, bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                Log.d("adManager", "[BANNER] AdErrorMode.PLATFORM showBanner>> $errorMode $errorMessage")
            }
        })
        adManager.showMrec(this, mrecContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                Log.d("adManager", "[MREC]AdErrorMode.PLATFORM showMrec>> $errorMode $errorMessage")
            }
        })
    }

    override fun onPause() {
        super.onPause()

        adManager.onPause(this)

    }


    override fun onDestroy() {
        super.onDestroy()

        adManager.destroy(this)
    }

    fun initViews() {
        btnShowInterstitial.setOnClickListener {
            adManager.showInterstitial(this) // if autoload mode is false it will load and show
        }

        btnShowRewarded.setOnClickListener {
            adManager.showRewarded(this) // if autoload mode is false it will load and show
        }

        btnShowInterstitialForTimeStrategy.setOnClickListener {
            adManager.showInterstitialForTimeStrategy(this)
        }

        btnOpenEmptyActivity.setOnClickListener {
            //startActivity(Intent(this, EmptyActivity::class.java))
            startActivity(Intent(this@MainActivity, JavaSampleActivity::class.java))
        }

        btnLoadAndShowInterstitial.setOnClickListener {
            adManager.loadAndShowInterstitial(this)
        }

        btnLoadAndShowRewarded.setOnClickListener {
            adManager.loadAndShowRewarded(this)
        }
    }

    /*
    var ADMOB_APP_ID = "ca-app-pub-8018256245650162~9841851144"
    var STARTAPP_APP_ID = "207754325"
    var IRONSOURCE_APP_ID = "a1a67f75"
    var MOPUB_APP_ID = "207754325"
     */
    fun initAdManager() {
        adManager = AdManager().apply {
            showAds = true
            autoLoad = true
            autoLoadDelay = 15 // seconds
            interstitialMinElapsedSecondsToNextShow = 60 // seconds
            randomInterval = 30 // random seconds for showing interstitial. Interstitial will show after previous showing passed seconds between 60-90
            testMode = BuildConfig.DEBUG
            deviceId = "47088e48-5195-4757-90b2-0da94116befd" // necessary if testmode enabled
            adPlatforms = mutableListOf<AdPlatformModel>(
                AdPlatformModel(
                    FacebookAdWrapper("your_app_id").apply {
                        interstitialPlacementId = "YOUR_PLACEMENT_ID"
                        bannerPlacementId = "YOUR_PLACEMENT_ID"
                        rewardedPlacementId = "YOUR_PLACEMENT_ID"
                        mrecPlacementId = "YOUR_PLACEMENT_ID"
                    },
                    true, true, true, true
                ),
                AdPlatformModel(
                    AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713").apply {
                        interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
                        bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
                        rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
                        mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
                    },
                    true, true, true, true
                ),

                AdPlatformModel(
                    IronSourceAdWrapper("cd353905").apply {
                        interstitialPlacementId = "DefaultInterstitial"
                        bannerPlacementId = "DefaultBanner"
                        rewardedPlacementId = "DefaultRewardedVideo"
                        mrecPlacementId = "MREC_BANNER"
                    },
                    true, true, true, true
                ),
                AdPlatformModel(
                    StartAppAdWrapper("207754325").apply {},
                    true, true, true, true
                )
            )
        }

        adManager.addAdPlatform(
            AdPlatformModel(
                MopubAdWrapper("207754325").apply {
                    interstitialPlacementId = "b75290feb5a74c79b2e7bf027a02f268"
                    bannerPlacementId = "1ac121edbb324cbf989df50731f69eae"
                    rewardedPlacementId = "7c13c6edc67a4fcab83e3cb45bd46597"
                    mrecPlacementId = "b311abd32a8944f4b6c6bba7fdb1f9e0"
                },
                true, true, true, true
            )
        )
        adManager.setAdPlatformSortByAdFormatStr("interstitial", "ironsource,mopub,admob,facebook")
        adManager.setAdPlatformSortByAdFormatStr("banner", "admob,ironsource,facebook,startapp,mopub")
        adManager.setAdPlatformSortByAdFormatStr("rewarded", "mopub,ironsource,admob,startapp,facebook")
        adManager.setAdPlatformSortByAdFormatStr("mrec", "admob,facebook,startapp,mopub,ironsource")


        adManager.globalInterstitialLoadListener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialLoadListener > $errorMessage")
                } else {
                    Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialLoadListener > $errorMessage")
                }
            }
        }
        adManager.globalRewardedLoadListener = object : AdPlatformLoadListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.MANAGER globalRewardedLoadListener > $errorMessage")
                } else {
                    Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.PLATFORM globalRewardedLoadListener > $errorMessage")
                }
            }
        }

        adManager.globalInterstitialShowListener = object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialShowListener > $errorMessage")
                } else {
                    Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialShowListener > $errorMessage")
                }
            }
        }

        adManager.globalRewardedShowListener = object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
                // AdErrorMode.MANAGER >> it means . We tried to load in all platforms but no one load interstitial
                if (errorMode == AdErrorMode.MANAGER) {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.MANAGER globalRewardedShowListener > $errorMessage")
                } else {
                    Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.PLATFORM globalRewardedShowListener > $errorMessage")
                }

            }
        }

        adManager.initialize(this)
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
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
            }
        })
        adManager.loadRewarded(object:AdPlatformLoadListener(){
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
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