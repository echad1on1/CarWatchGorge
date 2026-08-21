package com.dashboard.core.hardware.mock

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.hardware.Emitter
import com.dashboard.core.hardware.PhoneCommunication
import com.dashboard.core.hardware.Subscription

/**
 * Placeholder phone link. Emits nothing until the communication-protocol step wires it to
 * [MockBluetoothProvider] and adds developer controls (Start Navigation, Start Music, etc.).
 * Exists now purely so [com.dashboard.core.service.ConnectionManager] and future panel code
 * have a stable, already-correct dependency to compile against.
 */
class MockPhoneCommunication : PhoneCommunication {
    private val navigationEmitter = Emitter<NavigationState>()
    private val mediaEmitter = Emitter<MediaState>()
    private val blizzerEmitter = Emitter<BlizzerEvent>()

    override fun observeNavigationState(listener: (NavigationState) -> Unit): Subscription =
        navigationEmitter.subscribe(listener)

    override fun observeMediaState(listener: (MediaState) -> Unit): Subscription =
        mediaEmitter.subscribe(listener)

    override fun observeBlizzerEvents(listener: (BlizzerEvent) -> Unit): Subscription =
        blizzerEmitter.subscribe(listener)

    override fun sendMediaCommand(command: MediaCommand) {
        println("[MockPhoneCommunication] command sent (no-op until Music step): $command")
    }
}
