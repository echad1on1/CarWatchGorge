package com.dashboard.core.domain

/**
 * Lifecycle of the phone <-> dashboard link.
 *
 * The dashboard always starts in [CAR_ONLY]. A physical NFC tap (or, today,
 * [com.dashboard.core.hardware.mock.MockNfcProvider]) drives the transition
 * through [NFC_DETECTED] and [CONNECTING] into [CONNECTED]. Losing the phone
 * moves through [DISCONNECTING] back to [CAR_ONLY]. [ERROR] is reachable from
 * any in-progress transition and always recovers back to [CAR_ONLY].
 */
enum class ConnectionState {
    CAR_ONLY,
    NFC_DETECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR,
}

/** Which panels are reachable for a given [ConnectionState]. Pure function of state -> no UI logic duplicated. */
object PanelAvailability {
    fun isMapsAvailable(state: ConnectionState) = state == ConnectionState.CONNECTED
    fun isMusicAvailable(state: ConnectionState) = state == ConnectionState.CONNECTED
    // Car is always available, including mid-transition and on error.
    fun isCarAvailable(state: ConnectionState) = true
}
