
# Android Ad Manager
[![](https://jitpack.io/v/helikanon/androidadmanager.svg)](https://jitpack.io/#helikanon/androidadmanager)
[![API](https://img.shields.io/badge/API-19%2B-orange.svg?style=flat)](https://android-arsenal.com/api?level=19)

## Supported Ad Platforms
* [Facebook Audience](https://developers.facebook.com/docs/audience-network/get-started/android/)
* [Admob](https://developers.google.com/admob/android/quick-start)
* [Startapp](https://support.startapp.com/hc/en-us/articles/360002411114-Android-Standard-)
* [Ironsource](https://developers.ironsrc.com/ironsource-mobile/android/android-sdk/)
* [Mopub](https://developers.mopub.com/publishers/android/integrate/)

## Install

Put in "allprojects/repositories"
```java
maven { url "https://dl.bintray.com/ironsource-mobile/android-sdk" } // ironsource
maven {
	url "https://s3.amazonaws.com/moat-sdk-builds"

	// below line for fix = when using onesignal gradle sync erro
	content {  
		includeGroupByRegex "com\\.moat.*"  
	}
}
```

Put in "buildscript/dependencies" (for admob)
```java
classpath 'com.google.gms:google-services:4.3.3'
```

Put in app gradle
```java
implementation 'com.github.helikanon:androidadmanager:v1.3'
```

## Settings



### Manifest
```xml
<!-- Startapp -->  
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />  
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />  
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />  
<uses-permission android:name="android.permission.BLUETOOTH" />  
<!-- Startapp -->
```

### Gradle
Put in android/defaultConfig
```java
manifestPlaceholders = [
                admobAppId                     : 'your_admob_app_id'
        ]
```

## Usage

Initialize and load for next show
```kotlin
adManager = AdManager().apply {
    showAds = true
    autoLoadForInterstitial = false
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

        AdPlatformModel(
            AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713").apply {
                placementGroups.add(
                    AdPlacementGroupModel(
                        groupName = "default",
                        interstitial = "ca-app-pub-3940256099942544/1033173712",
                        rewarded = "ca-app-pub-3940256099942544/5224354917",
                        banner = "ca-app-pub-3940256099942544/6300978111",
                        mrec = "ca-app-pub-3940256099942544/6300978112",
                        native = "ca-app-pub-3940256099942544/2247696110",
                        appOpenAd = "ca-app-pub-3940256099942544/3419835294",
                        nativeMedium = ""
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
            IronSourceAdWrapper("cd353905").apply {
                placementGroups.add(
                    AdPlacementGroupModel(
                        groupName = "default",
                        interstitial = "DefaultInterstitial",
                        rewarded = "DefaultRewardedVideo",
                        banner = "DefaultBanner",
                        mrec = "MREC_BANNER",
                        native = "DefaultNative",
                        appOpenAd = "",
                        nativeMedium = ""

                    )
                )
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
        AdPlatformModel(
            ApplovinAdWrapper("noneed").apply {
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


adManager.setAdPlatformSortByAdFormatStr(0, "interstitial", "applovin,admob,ironsource")
adManager.setAdPlatformSortByAdFormatStr(0, "banner", "admob,applovin")
adManager.setAdPlatformSortByAdFormatStr(0, "rewarded", "ironsource,applovin")
adManager.setAdPlatformSortByAdFormatStr(0, "mrec", "admob,applovin")
adManager.setAdPlatformSortByAdFormatStr(0, "native", "admob,applovin")
adManager.setAdPlatformSortByAdFormatStr(0, "native_medium", "applovin")


adManager.globalInterstitialLoadListener = object : AdPlatformLoadListener() {
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
        if (errorMode == AdErrorMode.MANAGER) {
            Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialLoadListener > $errorMessage")
        } else {
            Log.d("adManager", "[LOAD][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialLoadListener > $errorMessage ${adPlatformEnum?.name}")
        }
    }
}
adManager.globalRewardedLoadListener = object : AdPlatformLoadListener() {
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
        if (errorMode == AdErrorMode.MANAGER) {
            Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.MANAGER globalRewardedLoadListener > $errorMessage")
        } else {
            Log.d("adManager", "[LOAD][REWARDED] AdErrorMode.PLATFORM globalRewardedLoadListener > $errorMessage ${adPlatformEnum?.name}")
        }
    }
}

adManager.globalInterstitialShowListener = object : AdPlatformShowListener() {
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
        if (errorMode == AdErrorMode.MANAGER) {
            Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.MANAGER globalInterstitialShowListener > $errorMessage")
        } else {
            Log.d("adManager", "[SHOW][INTERSTITIAL] AdErrorMode.PLATFORM globalInterstitialShowListener > $errorMessage ${adPlatformEnum?.name}")
        }
    }
}

adManager.globalRewardedShowListener = object : AdPlatformShowListener() {
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {
        // AdErrorMode.MANAGER >> it means . We tried to load in all platforms but no one load interstitial
        if (errorMode == AdErrorMode.MANAGER) {
            Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.MANAGER globalRewardedShowListener > $errorMessage")
        } else {
            Log.d("adManager", "[SHOW][REWARDED] AdErrorMode.PLATFORM globalRewardedShowListener > $errorMessage ${adPlatformEnum?.name}")
        }

    }
}

adManager.initializePlatforms(applicationContext)

```

### Manually initialize
```kotlin
// enable test mode
if (BuildConfig.DEBUG) {
    adManager.enableTestMode("47088e48-5195-4757-90b2-0da94116befd") // send device id, it is necessary for test facebook audience networks ad
}

adManager.loadInterstitial(object : AdPlatformLoadListener() {
    override fun onLoaded() {
    }
    
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?) {
    	if(AdErrorMode.MANAGER){
            // after tried all platforms to load 
            // this if run just one time after tried all platform and if not load any platform ad
        }else if(AdErrorMode.PLATFORM){
            // after each platform throw error
            // for example : if you 3 ad platform, here will run each platform error throw
        }
    }
    
})

adManager.loadRewarded(object : AdPlatformLoadListener() {
    override fun onLoaded() {
    }
    override fun onError(errorMode: AdErrorMode?, errorMessage: String?) { 
    	if(AdErrorMode.MANAGER){
            // after tried all platforms to load 
            // this if run just one time after tried all platform and if not load any platform ad
        }else if(AdErrorMode.PLATFORM){
            // after each platform throw error
            // for example : if you 3 ad platform, here will run each platform error throw
        }
    }
})
```




Show Ads
```kotlin
adManager.showBanner(bannerContainer) // bannerContainer is relativelayout
adManager.showMrec(mrecContainer) // mrecContainer is relativelayout

btnShowInterstitial.setOnClickListener {  
  adManager.showInterstitial()  
}  
  
btnShowRewarded.setOnClickListener {  
  adManager.showRewarded()  
}  
  
btnShowInterstitialForTimeStrategy.setOnClickListener {  
  adManager.showInterstitialForTimeStrategy()  
}
```
