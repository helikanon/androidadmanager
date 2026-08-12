package com.helikanonlib.admanager

import java.util.IdentityHashMap
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

internal fun interface DisplayLeaseTimeoutCancellation {
    fun cancel()
}

internal fun interface DisplayLeaseTimeoutScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): DisplayLeaseTimeoutCancellation
}

internal fun interface ReleasableDisplayLease {
    fun release()
}

/**
 * Owns full-screen display leases so they can also be released when an SDK omits
 * its terminal callback or the Activity/manager is destroyed.
 */
internal class FullScreenAdLeaseRegistry<Owner : Any>(
    private val timeoutScheduler: DisplayLeaseTimeoutScheduler,
    private val acquireGateLease: () -> AppOpenAdDisplayGate.Lease = AppOpenAdDisplayGate::acquire
) {
    private val lock = Any()
    private val leasesByOwner = IdentityHashMap<Owner, MutableSet<LeaseHandle>>()

    fun acquire(owner: Owner, timeoutMillis: Long): ReleasableDisplayLease {
        val lease = LeaseHandle(owner, acquireGateLease())
        synchronized(lock) {
            leasesByOwner.getOrPut(owner, ::mutableSetOf).add(lease)
        }
        lease.timeoutCancellation = timeoutScheduler.schedule(timeoutMillis.coerceAtLeast(1L)) {
            lease.release()
        }
        return lease
    }

    fun release(owner: Owner) {
        val leases = synchronized(lock) {
            leasesByOwner.remove(owner)?.toList().orEmpty()
        }
        leases.forEach { it.release() }
    }

    fun releaseAll() {
        val leases = synchronized(lock) {
            leasesByOwner.values.flatten().also { leasesByOwner.clear() }
        }
        leases.forEach { it.release() }
    }

    internal fun activeLeaseCount(): Int = synchronized(lock) {
        leasesByOwner.values.sumOf(Collection<LeaseHandle>::size)
    }

    private inner class LeaseHandle(
        private val owner: Owner,
        private val gateLease: AppOpenAdDisplayGate.Lease
    ) : ReleasableDisplayLease {
        private val isReleased = AtomicBoolean(false)
        @Volatile
        var timeoutCancellation: DisplayLeaseTimeoutCancellation? = null

        override fun release() {
            if (!isReleased.compareAndSet(false, true)) return

            timeoutCancellation?.cancel()
            gateLease.release()
            synchronized(lock) {
                leasesByOwner[owner]?.let { ownerLeases ->
                    ownerLeases.remove(this)
                    if (ownerLeases.isEmpty()) leasesByOwner.remove(owner)
                }
            }
        }
    }
}
