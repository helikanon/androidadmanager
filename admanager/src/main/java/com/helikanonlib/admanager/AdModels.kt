package com.helikanonlib.admanager




enum class AdPlatformTypeEnum {
    FACEBOOK,
    ADMOB,
    IRONSOURCE,
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
    NATIVE_MEDIUM,
    APP_OPEN,
}

enum class AdInitializationStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
    DISABLED
}

data class AdPlatformInitializationResult(
    val platform: AdPlatformTypeEnum,
    val isSuccessful: Boolean
)

data class AdInitializationResult(
    val status: AdInitializationStatus,
    val platformResults: List<AdPlatformInitializationResult> = emptyList(),
    val message: String? = null
) {
    val initializedPlatforms: List<AdPlatformTypeEnum>
        get() = platformResults.filter { it.isSuccessful }.map { it.platform }

    val failedPlatforms: List<AdPlatformTypeEnum>
        get() = platformResults.filterNot { it.isSuccessful }.map { it.platform }
}

data class AdPlatformError @JvmOverloads constructor(
    val format: AdFormatEnum,
    val platform: AdPlatformTypeEnum,
    val placementGroupIndex: Int,
    val message: String,
    val cause: Throwable? = null
)

data class AdManagerError @JvmOverloads constructor(
    val format: AdFormatEnum,
    val placementGroupIndex: Int,
    val attemptedPlatforms: List<AdPlatformTypeEnum> = emptyList(),
    val platformErrors: List<AdPlatformError> = emptyList(),
    val message: String
)


data class AdPlatformModel @JvmOverloads constructor(
    var platformInstance: AdPlatformWrapper,
    var showInterstitial: Boolean = false,
    var showBanner: Boolean = false,
    var showRewarded: Boolean = false,
    var showMrec: Boolean = false,
    var showNative: Boolean = false
)


data class AdPlacementGroupModel @JvmOverloads constructor(
    var groupName: String,
    var interstitial: String = "",
    var rewarded: String = "",
    var banner: String = "",
    var mrec: String = "",
    var native: String = "",
    var appOpenAd: String = "",
    var nativeMedium: String = ""


    /*var interstitialSort: ArrayList<AdPlatformTypeEnum>,
    var rewardedSort: ArrayList<AdPlatformTypeEnum>,
    var bannerSort: ArrayList<AdPlatformTypeEnum>,
    var mrecSort: ArrayList<AdPlatformTypeEnum>,
    var nativeSort: ArrayList<AdPlatformTypeEnum>,*/
)
