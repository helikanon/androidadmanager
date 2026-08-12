package com.helikanonlib.admanager

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal interface AppOpenAdDisplayState {
    val isBlocked: Boolean
}

internal object AppOpenAdDisplayGate : AppOpenAdDisplayState {
    private val activeFullScreenAdCount = AtomicInteger(0)

    override val isBlocked: Boolean
        get() = activeFullScreenAdCount.get() > 0

    fun acquire(): Lease {
        activeFullScreenAdCount.incrementAndGet()
        return Lease {
            activeFullScreenAdCount.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        }
    }

    internal class Lease(private val onRelease: () -> Unit) {
        private val isReleased = AtomicBoolean(false)

        fun release() {
            if (isReleased.compareAndSet(false, true)) onRelease()
        }
    }
}
