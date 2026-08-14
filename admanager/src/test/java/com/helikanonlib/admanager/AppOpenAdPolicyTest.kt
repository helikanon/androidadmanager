package com.helikanonlib.admanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {

    @Test
    fun `full screen ad activities are blocked from showing app open ads`() {
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.google.android.gms.ads.AdActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.applovin.adview.AppLovinFullscreenActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.applovin.adview.AppLovinFullscreenImmersiveActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.applovin.sdk.AppLovinWebViewActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.facebook.ads.AudienceNetworkActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.bytedance.sdk.openadsdk.activity.single.TTAdActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.fyber.inneractive.sdk.activities.InneractiveFullscreenAdActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.inmobi.ads.rendering.InMobiAdActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.mbridge.msdk.activity.MBCommonActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.unity3d.ads.adplayer.FullScreenWebViewDisplay"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.unity3d.services.ads.adunit.AdUnitActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.unity3d.services.ads.adunit.AdUnitTransparentActivity"))
        assertTrue(AppOpenAdPolicy.isFullScreenAdActivity("com.vungle.ads.internal.ui.VungleActivity"))
    }

    @Test
    fun `application activities are not mistaken for full screen ad activities`() {
        assertFalse(AppOpenAdPolicy.isFullScreenAdActivity("com.example.MainActivity"))
        assertFalse(AppOpenAdPolicy.isFullScreenAdActivity("com.example.AdActivity"))
        assertFalse(AppOpenAdPolicy.isFullScreenAdActivity("com.google.android.gms.ads.settings.AdsSettingsActivity"))
        assertFalse(AppOpenAdPolicy.isFullScreenAdActivity("com.applovin.mediation.MaxDebuggerActivity"))
    }

    @Test
    fun `first show is immediately allowed`() {
        assertTrue(AppOpenAdPolicy.hasShowIntervalElapsed(null, 10, 1_000L))
    }

    @Test
    fun `show is allowed exactly at minimum interval`() {
        assertTrue(AppOpenAdPolicy.hasShowIntervalElapsed(1_000L, 10, 11_000L))
    }

    @Test
    fun `show is blocked before minimum interval`() {
        assertFalse(AppOpenAdPolicy.hasShowIntervalElapsed(1_000L, 10, 10_999L))
    }

    @Test
    fun `negative interval behaves as zero`() {
        assertTrue(AppOpenAdPolicy.hasShowIntervalElapsed(1_000L, -10, 1_000L))
    }

    @Test
    fun `recently loaded ad is valid before expiration`() {
        assertTrue(AppOpenAdPolicy.wasLoadedRecently(1_000L, 10_000L, 10_999L))
    }

    @Test
    fun `loaded ad expires exactly at validity boundary`() {
        assertFalse(AppOpenAdPolicy.wasLoadedRecently(1_000L, 10_000L, 11_000L))
    }

    @Test
    fun `future or missing load timestamp is invalid`() {
        assertFalse(AppOpenAdPolicy.wasLoadedRecently(0L, 10_000L, 5_000L))
        assertFalse(AppOpenAdPolicy.wasLoadedRecently(6_000L, 10_000L, 5_000L))
    }
}
