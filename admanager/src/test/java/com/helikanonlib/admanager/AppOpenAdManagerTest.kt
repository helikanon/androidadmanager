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
        var errorMode: AdErrorMode? = null

        manager.load(loadListener(
            onLoaded = { loaded = true },
            onError = { errorMode = it }
        ))
        manager.disable()

        assertEquals(AdErrorMode.MANAGER, errorMode)
        adapter.completeLoad()
        assertFalse(loaded)
        assertEquals(1, adapter.invalidateCallCount)
    }

    @Test
    fun `load failure is forwarded with platform error mode`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var errorMode: AdErrorMode? = null

        manager.load(loadListener(onError = { errorMode = it }))
        adapter.failLoad("network")

        assertEquals(AdErrorMode.PLATFORM, errorMode)
    }

    @Test
    fun `disabled manager does not start adapter load`() {
        val adapter = FakeAppOpenAdAdapter()
        val manager = createManager(adapter)
        var errorMode: AdErrorMode? = null

        manager.disable()
        manager.load(loadListener(onError = { errorMode = it }))

        assertEquals(0, adapter.loadCallCount)
        assertEquals(AdErrorMode.MANAGER, errorMode)
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
        onError: (AdErrorMode?) -> Unit = {}
    ) = object : AdPlatformLoadListener() {
        override fun onLoaded(adPlatformEnum: AdPlatformTypeEnum?) {
            onLoaded()
        }

        override fun onError(
            errorMode: AdErrorMode?,
            errorMessage: String?,
            adPlatformEnum: AdPlatformTypeEnum?
        ) {
            onError(errorMode)
        }
    }
}

private class FakeAppOpenAdRuntime : AppOpenAdRuntime {
    override fun isMainThread() = true
    override fun post(action: () -> Unit) = action()
    override fun elapsedRealtime() = 1_000L
}

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
