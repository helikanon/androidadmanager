package com.helikanonlib.admanager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {

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
}
