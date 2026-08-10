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
