package com.helikanonlibsample.admanager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.*
import com.helikanonlib.admanager.adplatforms.ApplovinAdWrapper
import com.helikanonlibsample.admanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAds()
        initViews()


        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager.showNative(this@MainActivity, AdFormatEnum.NATIVE, binding.nativeContainer)
            MyApplication.adManager.showNative(this@MainActivity, AdFormatEnum.NATIVE_MEDIUM, binding.nativeMediumContainer)
        }, 15000)

        /*val x = MyApplication.adManager.getAdPlatformByType(AdPlatformTypeEnum.ADMOB)?.platformInstance as AdmobAdWrapper
        x.loadNativeAds(this, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
                x.showNative(this@MainActivity, 1, bannerContainer, "medium")
            }
        })*/

    }

    override fun onStart() {
        super.onStart()

        // App-open lifecycle is owned by the host app, not by the library.
        MyApplication.AppOpenAdManager?.onStart(this)
    }

    override fun onResume() {
        super.onResume()

        MyApplication.adManager.onResume(this)

        MyApplication.adManager.showBanner(this, binding.bannerContainer, object : AdPlatformShowListener() {
            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                Log.d("MyApplication.adManager", "[BANNER] AdErrorMode.PLATFORM showBanner>> $errorMode $errorMessage ${adPlatformEnum?.name}")
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager.showMrec(this, binding.mrecContainer, object : AdPlatformShowListener() {
                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.d("MyApplication.adManager", "[MREC] AdErrorMode.PLATFORM showMrec>> $errorMode $errorMessage ${adPlatformEnum?.name}")
                }
            })
        }, 3000)
    }

    override fun onPause() {
        super.onPause()

        MyApplication.adManager.onPause(this)

    }


    override fun onDestroy() {
        super.onDestroy()

        MyApplication.adManager.destroy(this)
    }

    fun initViews() {
        binding.btnShowInterstitial.setOnClickListener {
            MyApplication.adManager.showInterstitial(this, "btnShowInterstitial", object : AdPlatformShowListener() {
                override fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onDisplayed(adPlatformEnum)
                }

                override fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onClicked(adPlatformEnum)
                }

                override fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onClosed(adPlatformEnum)
                }

                override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onRewarded(type, amount, adPlatformEnum)
                }

                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onError(errorMode, errorMessage, adPlatformEnum)
                }

            }) // if autoload mode is false it will load and show
        }

        binding.btnShowRewarded.setOnClickListener {
            MyApplication.adManager.showRewarded(this, object : AdPlatformShowListener() {
                override fun onRewarded(type: String?, amount: Int?, adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onRewarded(type, amount, adPlatformEnum)
                }
            }) // if autoload mode is false it will load and show
        }

        binding.btnShowInterstitialForTimeStrategy.setOnClickListener {
            MyApplication.adManager.showInterstitialForTimeStrategy(this)
        }

        binding.btnOpenEmptyActivity.setOnClickListener {
            //startActivity(Intent(this, EmptyActivity::class.java))
            startActivity(Intent(this@MainActivity, JavaSampleActivity::class.java))
        }

        binding.btnLoadAndShowInterstitial.setOnClickListener {
            MyApplication.adManager.loadAndShowInterstitial(this)
        }

        binding.btnLoadAndShowRewarded.setOnClickListener {
            MyApplication.adManager.loadAndShowRewarded(this)
        }

        binding.btnLoadAppOpenAd.setOnClickListener {
            MyApplication.AppOpenAdManager?.show("admob,applovin", this, null)

            //MyApplication.admobAppOpenAdManager?.disable()
        }
        binding.btnApplovinDebugger.setOnClickListener {
            MyApplication.adManager.applovinDebugger(this@MainActivity)

        }
    }

    /*
    var ADMOB_APP_ID = "ca-app-pub-8018256245650162~9841851144"

    var IRONSOURCE_APP_ID = "a1a67f75"
    var MOPUB_APP_ID = "207754325"
     */
    fun initAds() {
        MyApplication.adManager.initializePlatformsWithActivity(this)
        MyApplication.adManager.start(this)
        MyApplication.adManager.loadNativeAds(this, AdFormatEnum.NATIVE, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)
            }

        })

        MyApplication.adManager.loadNativeAds(this, AdFormatEnum.NATIVE_MEDIUM, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
            }

            override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                super.onError(errorMode, errorMessage, adPlatformEnum)
            }

        })
        /*Handler(Looper.getMainLooper()).postDelayed({
            MyApplication.adManager.showInterstitial(this@MainActivity)
        }, 2000)*/
    }
}
