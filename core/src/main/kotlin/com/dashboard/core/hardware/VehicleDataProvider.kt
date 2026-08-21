package com.dashboard.core.hardware

import com.dashboard.core.domain.VehicleData

/**
 * Source of truth for [VehicleData]. A real implementation will speak OBD-II/CAN (or whatever
 * interface the target vehicle exposes); nothing above this interface may know that.
 *
 * The UI layer must never depend on this interface directly — it goes through
 * [com.dashboard.core.service.VehicleDataManager].
 */
interface VehicleDataProvider {
    /** Begin producing data. Safe to call multiple times; subsequent calls are no-ops. */
    fun start()

    /** Stop producing data and release any underlying resources. */
    fun stop()

    /** Register for updates. Returns a subscription handle that can be used to unsubscribe. */
    fun observe(listener: (VehicleData) -> Unit): Subscription
}

/** Cancellable handle returned by observe()-style APIs across the hardware layer. */
fun interface Subscription {
    fun cancel()
}
