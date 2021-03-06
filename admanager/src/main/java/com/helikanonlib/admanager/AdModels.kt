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