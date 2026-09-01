package com.dashboard.wearos.hardware

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives phone → watch nav messages on [WearDataLayerBluetoothProvider.NAV_MESSAGE_PATH]
 * and forwards raw bytes into the shared [WearDataLayerBluetoothProvider].
 */
class NavDataListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearDataLayerBluetoothProvider.NAV_MESSAGE_PATH) return
        WearDataLayerBluetoothProvider.getInstance(this).pushInbound(messageEvent.data)
    }
}
