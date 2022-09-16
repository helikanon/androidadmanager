package com.helikanonlib.admanager


enum class AdErrorMode {
    MANAGER,
    PLATFORM
}


enum class AdPlatformTypeEnum {
    FACEBOOK,
    ADMOB,
    STARTAPP,
    ADINCUBE,
    IRONSOURCE,
    MOPUB,
    UNITYADS,
    ADMOST,
    APPLOVIN
}

enum class AdFormatEnum {
    INTERSTITIAL,
    BANNER,
    REWARDED,
    MREC,
    NATIVE,
}


data class AdPlatformModel @JvmOverloads constructor(
    var platformInstance: AdPlatformWrapper,
    var showInterstitial: Boolean = false,
    var showBanner: Boolean = false,
    var showRewarded: Boolean = false,
    var showMrec: Boolean = false
)


data class AdPlacementGroupModel @JvmOverloads constructor(
    var groupName: String,
    var interstitial: String = "",
    var rewarded: String = "",
    var banner: String = "",
    var mrec: String = "",
    var native: String = "",
    var appOpenAd: String = "",

    /*var interstitialSort: ArrayList<AdPlatformTypeEnum>,
    var rewardedSort: ArrayList<AdPlatformTypeEnum>,
    var bannerSort: ArrayList<AdPlatformTypeEnum>,
    var mrecSort: ArrayList<AdPlatformTypeEnum>,
    var nativeSort: ArrayList<AdPlatformTypeEnum>,*/
)