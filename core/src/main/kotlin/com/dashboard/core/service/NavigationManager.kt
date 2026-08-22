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
 */
class NavigationManager(private val phoneCommunication: PhoneCommunication) {

    private val emitter = Emitter<NavigationState>()

    var latest: NavigationState = NavigationState.INACTIVE
        private set

    private var sub: Subscription? = null

    fun start() {
        sub = phoneCommunication.observeNavigationState { state ->
            latest = state
            emitter.emit(state)
        }
    }

    fun stop() {
        sub?.cancel()
        // Navigation data belongs to the phone; once we stop listening, don't keep showing
        // possibly-stale info if this manager is ever restarted later in the same session.
        latest = NavigationState.INACTIVE
    }

    fun observe(listener: (NavigationState) -> Unit): Subscription {
        listener(latest)
        return emitter.subscribe(listener)
    }
}
