package com.helikanonlib.admanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdDisplayGateTest {

    @Test
    fun `gate remains blocked until every active full screen ad is released`() {
        val firstLease = AppOpenAdDisplayGate.acquire()
        val secondLease = AppOpenAdDisplayGate.acquire()

        assertTrue(AppOpenAdDisplayGate.isBlocked)

        firstLease.release()
        assertTrue(AppOpenAdDisplayGate.isBlocked)

        secondLease.release()
        assertFalse(AppOpenAdDisplayGate.isBlocked)
    }

    @Test
    fun `releasing the same lease more than once is safe`() {
        val lease = AppOpenAdDisplayGate.acquire()

        lease.release()
        lease.release()

        assertFalse(AppOpenAdDisplayGate.isBlocked)
    }

    @Test
    fun `gate records when a full screen ad closes`() {
        AppOpenAdDisplayGate.recordFullScreenAdClosed(nowElapsedRealtime = 12_345L)

        assertTrue(AppOpenAdDisplayGate.lastFullScreenAdClosedElapsedRealtime == 12_345L)
    }
}
