package com.helikanonlib.admanager


abstract class AdPlatformLoadListener() {
    open fun onLoaded() {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?) {}
}


abstract class AdPlatformShowListener() {
    open fun onDisplayed() {}
    open fun onClicked() {}
    open fun onClosed() {}
    open fun onRewarded(type: String? = null, amount: Int? = null) {}
    open fun onError(errorMode: AdErrorMode?, errorMessage: String?) {}
}



