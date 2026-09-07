package com.dashboard.wearos.hardware

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Fallback inbound path: the system starts this service to deliver a Data Layer message when
 * the watch app process is not already alive. When the app *is* alive,
 * [WearDataLayerBluetoothProvider]'s own [com.google.android.gms.wearable.MessageClient]
 * listener handles delivery and this service's callback is redundant (the provider dedups via
 * its replay buffer only for ordering, not identity — a duplicate frame decodes to the same
 * protocol message and is harmless).
 */
class NavDataListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (!event.path.startsWith(MESSAGE_PATH_PREFIX)) return
        WearDataLayerBluetoothProvider.pushInbound(applicationContext, event.data)
    }
}
