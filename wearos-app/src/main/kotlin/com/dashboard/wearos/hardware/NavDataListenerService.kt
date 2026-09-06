package com.dashboard.wearos.hardware

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * The system can start this service to deliver a Data Layer message even when the watch app
 * isn't running in the foreground.
 */
class NavDataListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != MESSAGE_PATH) return
        WearDataLayerBluetoothProvider.pushInbound(event.data)
    }
}
