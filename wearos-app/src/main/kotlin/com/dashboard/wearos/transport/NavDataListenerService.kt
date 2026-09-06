package com.dashboard.wearos.transport

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class NavDataListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != MESSAGE_PATH) return
        WearDataLayerBluetoothProvider.pushInbound(event.data)
    }
}
