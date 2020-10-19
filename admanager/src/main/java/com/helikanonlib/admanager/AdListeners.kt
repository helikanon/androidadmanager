package com.helikanonlib.admanager


abstract class AdPlatformLoadListener() {
    open fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {}
}


abstract class AdPlatformShowListener() {
    open fun onDisplayed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClicked(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onClosed(adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onRewarded(type: String? = null, amount: Int? = null, adPlatformEnum: AdPlatformTypeEnum?) {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?, adPlatformEnum: AdPlatformTypeEnum?) {}
}



