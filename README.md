
# androidadmanager
[![](https://jitpack.io/v/helikanon/androidadmanager.svg)](https://jitpack.io/#helikanon/androidadmanager)

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
implementation 'com.github.helikanon:androidadmanager:v1.0'
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

## Supported Ad Platforms
* [Facebook Audience](https://developers.facebook.com/docs/audience-network/get-started/android/)
* [Admob](https://developers.google.com/admob/android/quick-start)
* [Startapp](https://support.startapp.com/hc/en-us/articles/360002411114-Android-Standard-)
* [Ironsource](https://developers.ironsrc.com/ironsource-mobile/android/android-sdk/)
* [Mopub](https://developers.mopub.com/publishers/android/integrate/)

## Usage

Initialize and load for next show
```kotlin
val adManager = AdManager().apply {
    activity = this@MainActivity
    context = applicationContext
    showAds = true
    autoLoad = true
    autoLoadDelay = 15 // seconds
    randomInterval = 30 // seconds
    interstitialMinElapsedSecondsToNextShow = 60 // seconds
    adPlatforms = mutableListOf<AdPlatformModel>(
        AdPlatformModel(
            FacebookAdWrapper("your_app_id", this@MainActivity, applicationContext).apply {
                interstitialPlacementId = "YOUR_PLACEMENT_ID"
                bannerPlacementId = "YOUR_PLACEMENT_ID"
                rewardedPlacementId = "YOUR_PLACEMENT_ID"
                mrecPlacementId = "YOUR_PLACEMENT_ID"
            },
            true,true,true,true
        ),
        AdPlatformModel(
            AdmobAdWrapper("ca-app-pub-3940256099942544~3347511713", this@MainActivity, applicationContext).apply {
                interstitialPlacementId = "ca-app-pub-3940256099942544/1033173712"
                bannerPlacementId = "ca-app-pub-3940256099942544/6300978111"
                rewardedPlacementId = "ca-app-pub-3940256099942544/5224354917"
                mrecPlacementId = "ca-app-pub-3940256099942544/6300978111"
            },
            true,true,true,true
        ),
        AdPlatformModel(
            StartAppAdWrapper("207754325", this@MainActivity, applicationContext).apply {

            },
            false,true,true,true
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
            true,true,true,false
        )
    )
}

adManager.initializePlatforms()

if (BuildConfig.DEBUG) {
    adManager.enableTestMode("47088e48-5195-4757-90b2-0da94116befd") // send device id, it is necessary for test facebook audience networks ad
}

// adManager.start() // OR below manually start to load
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
