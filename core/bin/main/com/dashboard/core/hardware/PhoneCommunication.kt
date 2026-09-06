package com.dashboard.core.hardware

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.NavigationState

/**
 * The dashboard's view of "a connected phone" — transport-independent. A [BluetoothProvider]
 * carries the bytes; something in the communication layer decodes/encodes them into calls on
 * this interface. Panels depend on this, never on Bluetooth.
 *
 * Full message shapes are defined in `com.dashboard.core.communication` (added in the
 * communication-protocol step). This interface is intentionally minimal for now — just enough
 * for the connection state machine and Car panel to compile against a stable contract.
 */
interface PhoneCommunication {
    fun observeNavigationState(listener: (NavigationState) -> Unit): Subscription
    fun observeMediaState(listener: (MediaState) -> Unit): Subscription
    fun observeBlizzerEvents(listener: (BlizzerEvent) -> Unit): Subscription
    fun sendMediaCommand(command: MediaCommand)
}
