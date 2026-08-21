package com.dashboard.core.service

import com.dashboard.core.domain.VehicleData
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.Subscription
import com.dashboard.core.hardware.VehicleDataProvider

/**
 * The Car panel's ONLY dependency for vehicle data. It never touches [VehicleDataProvider]
 * (or, later, OBD-II/CAN) directly. This indirection is what lets the mock simulator be swapped
 * for a real vehicle interface without changing the UI at all.
 *
 * Also caches the latest snapshot so a UI that mounts after data starts flowing (e.g. rotating
 * through panels back to Car) can render immediately instead of waiting for the next tick.
 */
class VehicleDataManager(private val provider: VehicleDataProvider) {

    private val emitter = Emitter<VehicleData>()

    var latest: VehicleData = VehicleData()
        private set

    private var providerSub: Subscription? = null

    fun start() {
        provider.start()
        providerSub = provider.observe { data ->
            latest = data
            emitter.emit(data)
        }
    }

    fun stop() {
        providerSub?.cancel()
        provider.stop()
    }

    fun observe(listener: (VehicleData) -> Unit): Subscription {
        listener(latest) // deliver current snapshot immediately, same rationale as ConnectionManager
        return emitter.subscribe(listener)
    }
}
