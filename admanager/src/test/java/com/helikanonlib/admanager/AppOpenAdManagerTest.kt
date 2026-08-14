package com.helikanonlib.admanager

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdManagerTest {

    @Test
    fun `concurrent load requests share one adapter load and notify every listener`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var firstLoaded = false
        var secondLoaded = false

        manager.load(loadListener(onLoaded = { firstLoaded = true }))
        manager.load(loadListener(onLoaded = { secondLoaded = true }))

        assertEquals(1, adapter.loadCallCount)
        adapter.completeLoad()
        assertTrue(firstLoaded)
        assertTrue(secondLoaded)
    }

    @Test
    fun `disable completes pending listener and invalidates late load result`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var loaded = false
        var managerError: AdManagerError? = null

        manager.load(loadListener(
            onLoaded = { loaded = true },
            onError = { managerError = it }
        ))
        manager.disable()

        assertEquals(AdFormatEnum.APP_OPEN, managerError?.format)
        adapter.completeLoad()
        assertFalse(loaded)
        assertEquals(1, adapter.invalidateCallCount)
    }

    @Test
    fun `load failure reports platform detail before terminal manager error`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var platformError: AdPlatformError? = null
        var managerError: AdManagerError? = null

        manager.load(loadListener(
            onPlatformError = { platformError = it },
            onError = { managerError = it }
        ))
        adapter.failLoad("network")

        assertEquals(AdPlatformTypeEnum.ADMOB, platformError?.platform)
        assertEquals("network", platformError?.message)
        assertEquals(listOf(platformError), managerError?.platformErrors)
    }

    @Test
    fun `disabled manager does not start adapter load`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var managerError: AdManagerError? = null

        manager.disable()
        manager.load(loadListener(onError = { managerError = it }))

        assertEquals(0, adapter.loadCallCount)
        assertEquals(AdFormatEnum.APP_OPEN, managerError?.format)
    }

    @Test
    fun `full screen cooldown uses the configured duration`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = AppOpenAdManager(
            adapters = listOf(adapter),
            runtime = FakeAppOpenAdRuntime(),
            showOrderStr = "admob",
            displayState = FakeAppOpenAdDisplayState(lastFullScreenAdClosedElapsedRealtime = 1_000L)
        )
        manager.minElapsedSecondsAfterFullScreenAd = 30

        assertFalse(manager.hasFullScreenAdIntervalElapsed(nowElapsedRealtime = 30_999L))
        assertTrue(manager.hasFullScreenAdIntervalElapsed(nowElapsedRealtime = 31_000L))
    }

    private fun createManager(adapter: FakeAppOpenAdAdapter): AppOpenAdManager {
        return AppOpenAdManager(
            adapters = listOf(adapter),
            runtime = FakeAppOpenAdRuntime(),
            showOrderStr = "admob"
        )
    }

    private fun loadListener(
        onLoaded: () -> Unit = {},
        onPlatformError: (AdPlatformError) -> Unit = {},
        onError: (AdManagerError) -> Unit = {}
    ) = object : AdPlatformLoadListener() {
        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum) {
            onLoaded()
        }

        override fun onPlatformError(error: AdPlatformError) {
            onPlatformError(error)
        }

        override fun onError(error: AdManagerError) {
            onError(error)
        }
    }
}

private class FakeAppOpenAdRuntime : AppOpenAdRuntime {
    override fun isMainThread() = true
    override fun post(action: () -> Unit) = action()
    override fun elapsedRealtime() = 1_000L
}

private class FakeAppOpenAdDisplayState(
    override val isBlocked: Boolean = false,
    override val lastFullScreenAdClosedElapsedRealtime: Long? = null
) : AppOpenAdDisplayState

private class FakeAppOpenAdAdapter : AppOpenAdAdapter {
    override val platform = AdPlatformTypeEnum.ADMOB

    var loadCallCount = 0
        private set
    var invalidateCallCount = 0
        private set

    private var isReady = false
    private var loadCallback: AppOpenAdLoadCallback? = null

    override fun isReady(validityDurationMillis: Long, nowElapsedRealtime: Long) = isReady

    override fun load(callback: AppOpenAdLoadCallback) {
        loadCallCount++
        loadCallback = callback
    }

    override fun show(activity: Activity, callback: AppOpenAdDisplayCallback) = isReady

    override fun invalidate() {
        invalidateCallCount++
        isReady = false
    }

    fun completeLoad() {
        isReady = true
        loadCallback?.onLoaded()
    }

    fun failLoad(message: String) {
        loadCallback?.onError(message)
    }
}
