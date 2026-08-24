package com.dashboard.core.service

import com.dashboard.core.domain.NavigationState
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * The Maps panel's ONLY dependency for navigation data. Mirrors [VehicleDataManager]'s shape:
 * caches the latest snapshot so a panel that mounts after data starts flowing (e.g. scrolling
 * back to Maps) renders immediately, and delivers that snapshot to late subscribers.
 *
 * Destination search and route calculation stay entirely on the phone — this manager only ever
 * relays what [PhoneCommunication] reports. When `active == false`, the Maps panel must show
 * "navigation not running", never stale data from a previous session.
 *
 * ## Countdown interpolation
 * Real navigation data (see `docs/android-integration-research.md` and
 * [com.dashboard.core.communication.NavigationAnnouncementParser]) arrives as discrete spoken-
 * announcement checkpoints ("next turn is 200m away right now"), not a continuous feed. Call
 * [onVehicleSpeedTick] on every vehicle-data update (the composition root wires this to
 * [VehicleDataManager]) to smoothly count [NavigationState.distanceMeters] down between
 * checkpoints using the vehicle's own live speed — deliberately the *vehicle's* speed rather than
 * a separate phone-GPS reading, since it's already flowing through this app and is at least as
 * accurate. Any new checkpoint (from [PhoneCommunication], including
 * [com.dashboard.core.hardware.mock.MockPhoneCommunication.announceNavigation]) resets the
 * interpolation baseline, so a fresh announcement always wins over accumulated estimation drift.
 */
class NavigationManager(private val phoneCommunication: PhoneCommunication) {

    private val emitter = Emitter<NavigationState>()

    var latest: NavigationState = NavigationState.INACTIVE
        private set

    private var sub: Subscription? = null
    private var lastTickTimestampMillis: Long? = null

    fun start() {
        sub = phoneCommunication.observeNavigationState { state -> setAuthoritativeState(state) }
    }

    fun stop() {
        sub?.cancel()
        // Navigation data belongs to the phone; once we stop listening, don't keep showing
        // possibly-stale info if this manager is ever restarted later in the same session.
        latest = NavigationState.INACTIVE
        lastTickTimestampMillis = null
    }

    fun observe(listener: (NavigationState) -> Unit): Subscription {
        listener(latest)
        return emitter.subscribe(listener)
    }

    /**
     * Feed the vehicle's current speed in on every tick (the composition root calls this from
     * [VehicleDataManager]'s observer). Decrements [NavigationState.distanceMeters] by however
     * far the vehicle has traveled since the last tick, floored at 0. A no-op while navigation
     * isn't active, or before a real checkpoint has established a baseline.
     */
    fun onVehicleSpeedTick(speedKmh: Double, nowMillis: Long = System.currentTimeMillis()) {
        val state = latest
        if (!state.active) {
            lastTickTimestampMillis = null
            return
        }
        val distance = state.distanceMeters
        val previousTick = lastTickTimestampMillis
        lastTickTimestampMillis = nowMillis
        if (distance == null || previousTick == null) return // nothing to interpolate from yet

        val elapsedSeconds = (nowMillis - previousTick) / 1000.0
        if (elapsedSeconds <= 0) return

        val speedMetersPerSecond = speedKmh / 3.6
        val newDistance = (distance - speedMetersPerSecond * elapsedSeconds).coerceAtLeast(0.0)
        if (newDistance == distance) return

        latest = state.copy(distanceMeters = newDistance)
        emitter.emit(latest)
    }

    private fun setAuthoritativeState(state: NavigationState) {
        latest = state
        // Any authoritative update (a fresh checkpoint, a full state push, stop/start) resets the
        // interpolation baseline so estimation drift never outlives real information.
        lastTickTimestampMillis = null
        emitter.emit(state)
    }
}
