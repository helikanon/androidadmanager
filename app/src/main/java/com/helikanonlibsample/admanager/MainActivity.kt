package com.helikanonlibsample.admanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.AdManager
import com.helikanonlib.admanager.AdPlatformLoadListener
import com.helikanonlib.admanager.AdPlatformModel
import com.helikanonlib.admanager.adplatforms.FacebookAdWrapper
import com.helikanonlib.admanager.adplatforms.IronSourceAdWrapper
import com.helikanonlib.admanager.adplatforms.MopubAdWrapper
import com.helikanonlib.admanager.adplatforms.StartAppAdWrapper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAdManager()

        initViews()
        adManager.showBanner(bannerContainer)
        adManager.showMrec(mrecContainer)

    }

    fun initViews() {
        btnShowInterstitial.setOnClickListener {
            adManager.showInterstitial()
        }

        btnShowRewarded.setOnClickListener {
            adManager.showRewarded()
        }

        btnShowInterstitialForTimeStrategy.setOnClickListener {
            adManager.showInterstitialForTimeStrategy()
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
            activity = this@MainActivity
            context = applicationContext
            showAds = true
            autoLoad = true
            autoLoadDelay = 15 // seconds
            randomInterval = 30 // seconds
            interstitialMinElapsedSecondsToNextShow = 60 // seconds
            adPlatforms = mutableListOf<AdPlatformModel>(
                AdPlatformModel(
                    FacebookAdWrapper("", this@MainActivity, applicationContext).apply {
                        interstitialPlacementId = "YOUR_PLACEMENT_ID"
                        bannerPlacementId = "YOUR_PLACEMENT_ID"
                        rewardedPlacementId = "YOUR_PLACEMENT_ID"
                        mrecPlacementId = "YOUR_PLACEMENT_ID"
                    },
                    true,true,true,true
                ),
                /*AdPlatformModel(
                    AdmobAdWrapper("test", this@MainActivity, applicationContext).apply {
                        interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
                        bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
                        rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
                        mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
                    },
                    false,
                    false,
                    false,
                    false
                ),*/
                AdPlatformModel(
                    StartAppAdWrapper("207754325", this@MainActivity, applicationContext).apply {

                    },
                    true,true,true,true
                ),
                AdPlatformModel(
                    IronSourceAdWrapper("a1a67f75", this@MainActivity, applicationContext).apply {
                        interstitialPlacementId = "DefaultInterstitial"
                        bannerPlacementId = "DefaultBanner"
                        rewardedPlacementId = "DefaultRewardedVideo"
                        mrecPlacementId = "MREC_BANNER"
                    },
                    true,true,true,true
                ),
                AdPlatformModel(
                    MopubAdWrapper("207754325", this@MainActivity, applicationContext).apply {
                        interstitialPlacementId = "b75290feb5a74c79b2e7bf027a02f268"
                        bannerPlacementId = "1ac121edbb324cbf989df50731f69eae"
                        rewardedPlacementId = "7c13c6edc67a4fcab83e3cb45bd46597"
                        mrecPlacementId = "b311abd32a8944f4b6c6bba7fdb1f9e0"
                    },
                    true,true,true,true
                )
            )
        }

        adManager.initializePlatforms()

        if (BuildConfig.DEBUG) {
            adManager.enableTestMode("47088e48-5195-4757-90b2-0da94116befd") // send device id, it is necessary for test facebook audience networks ad
        }

        // adManager.start() // start to load interstitial and rewarded video for next show
        adManager.loadInterstitial(object : AdPlatformLoadListener() {
            override fun onLoaded() {
                super.onLoaded()
            }
        })

        adManager.loadRewarded(object : AdPlatformLoadListener() {
            override fun onLoaded() {
                super.onLoaded()
            }
        })
    }
}