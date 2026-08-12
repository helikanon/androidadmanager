package com.helikanonlib.admanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullScreenAdLeaseRegistryTest {

    @Test
    fun `terminal callback releases lease`() {
        val scheduler = FakeDisplayLeaseTimeoutScheduler()
        val registry = FullScreenAdLeaseRegistry<Any>(scheduler)

        val lease = registry.acquire(Any(), 1_000L)
        assertTrue(AppOpenAdDisplayGate.isBlocked)

        lease.release()

        assertFalse(AppOpenAdDisplayGate.isBlocked)
        assertEquals(0, registry.activeLeaseCount())
        assertTrue(scheduler.tasks.single().isCancelled)
    }

    @Test
    fun `owner destruction releases only its leases`() {
        val scheduler = FakeDisplayLeaseTimeoutScheduler()
        val registry = FullScreenAdLeaseRegistry<Any>(scheduler)
        val firstOwner = Any()
        val secondOwner = Any()

        registry.acquire(firstOwner, 1_000L)
        registry.acquire(secondOwner, 1_000L)

        registry.release(firstOwner)

        assertTrue(AppOpenAdDisplayGate.isBlocked)
        assertEquals(1, registry.activeLeaseCount())

        registry.release(secondOwner)
        assertFalse(AppOpenAdDisplayGate.isBlocked)
    }

    @Test
    fun `timeout releases lease when SDK sends no terminal callback`() {
        val scheduler = FakeDisplayLeaseTimeoutScheduler()
        val registry = FullScreenAdLeaseRegistry<Any>(scheduler)

        registry.acquire(Any(), 1_000L)
        assertTrue(AppOpenAdDisplayGate.isBlocked)

        scheduler.tasks.single().run()

        assertFalse(AppOpenAdDisplayGate.isBlocked)
        assertEquals(0, registry.activeLeaseCount())
    }

    @Test
    fun `manager destruction releases every owned lease`() {
        val scheduler = FakeDisplayLeaseTimeoutScheduler()
        val registry = FullScreenAdLeaseRegistry<Any>(scheduler)

        registry.acquire(Any(), 1_000L)
        registry.acquire(Any(), 1_000L)

        registry.releaseAll()

        assertFalse(AppOpenAdDisplayGate.isBlocked)
        assertEquals(0, registry.activeLeaseCount())
    }

    private class FakeDisplayLeaseTimeoutScheduler : DisplayLeaseTimeoutScheduler {
        val tasks = mutableListOf<ScheduledTask>()

        override fun schedule(
            delayMillis: Long,
            action: () -> Unit
        ): DisplayLeaseTimeoutCancellation {
            return ScheduledTask(action).also(tasks::add)
        }
    }

    private class ScheduledTask(private val action: () -> Unit) : DisplayLeaseTimeoutCancellation {
        var isCancelled = false
            private set

        override fun cancel() {
            isCancelled = true
        }

        fun run() {
            if (!isCancelled) action()
        }
    }
}
