package com.helikanonlib.admanager

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal interface AppOpenAdDisplayState {
    val isBlocked: Boolean
    val lastFullScreenAdClosedElapsedRealtime: Long?
}

internal object AppOpenAdDisplayGate : AppOpenAdDisplayState {
    private val activeFullScreenAdCount = AtomicInteger(0)
    private val lastFullScreenAdClosedAt = AtomicLong(NO_RECORDED_CLOSE)

    override val isBlocked: Boolean
        get() = activeFullScreenAdCount.get() > 0

    override val lastFullScreenAdClosedElapsedRealtime: Long?
        get() = lastFullScreenAdClosedAt.get().takeUnless { it == NO_RECORDED_CLOSE }

    fun acquire(): Lease {
        activeFullScreenAdCount.incrementAndGet()
        return Lease {
            activeFullScreenAdCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        }
    }

    fun recordFullScreenAdClosed(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()) {
        lastFullScreenAdClosedAt.set(nowElapsedRealtime)
    }

    internal class Lease(private val onRelease: () -> Unit) {
        private val isReleased = AtomicBoolean(false)

        fun release() {
            if (isReleased.compareAndSet(false, true)) onRelease()
        }
    }

    private const val NO_RECORDED_CLOSE = Long.MIN_VALUE
}
