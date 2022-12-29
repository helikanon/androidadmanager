package com.helikanonlibsample.admanager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.helikanonlib.admanager.*
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
            MyApplication.adManager?.showNative(this@MainActivity, AdFormatEnum.NATIVE, binding.nativeContainer)
        }, 5000)


        Handler(Looper.getMainLooper()).postDelayed({
            // MyApplication.admobAppOpenAdManager?.show(this@MainActivity, null)


            /*val ap = MyApplication.adManager?.getAdPlatformByType(AdPlatformTypeEnum.ADMOB)
            ap?.platformInstance?.loadNativeAds(this@MainActivity, 4, object : AdPlatformLoadListener() {
                override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                    super.onLoaded(adPlatformEnum)

                    runOnUiThread {

                        val nativeAd = ap?.platformInstance?.showNative(
                            this@MainActivity,
                            "medium",
                            binding.nativeContainer
                        )

                        val inflater = this@MainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                        val templateRoot = inflater.inflate(R.layout.admob_native_medium_template, null)
                        val template = templateRoot.findViewById<TemplateView>(R.id.admanager_native_medium)

                        val nativeAd = ap?.platformInstance?.showNative(
                            this@MainActivity,
                            0
                        )
                        nativeAd?.let {
                            val styles = NativeTemplateStyle.Builder().build()
                            template.setStyles(styles)
                            template.setNativeAd(nativeAd)
                            binding.nativeContainer.addView(templateRoot)
                        }
                    }
                }

                override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
                    Log.d("MyApplication.adManager", "[NATIVE] ${errorMode?.name} / $errorMessage / ${adPlatformEnum?.name}")
                }
            })*/

            MyApplication.admobAppOpenAdManager?.isEnable = true
        }, 2000)


        /*val x = MyApplication.adManager.getAdPlatformByType(AdPlatformTypeEnum.ADMOB)?.platformInstance as AdmobAdWrapper
        x.loadNativeAds(this, 3, object : AdPlatformLoadListener() {
            override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
                super.onLoaded(adPlatformEnum)
                x.showNative(this@MainActivity, 1, bannerContainer, "medium")
            }
        })*/

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
            MyApplication.adManager.showInterstitial(this, object : AdPlatformShowListener() {
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
            MyApplication.admobAppOpenAdManager?.showAdmob(this, null)

            //MyApplication.admobAppOpenAdManager?.disable()
        }
    }

    /*
    var ADMOB_APP_ID = "ca-app-pub-8018256245650162~9841851144"
    var STARTAPP_APP_ID = "207754325"
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