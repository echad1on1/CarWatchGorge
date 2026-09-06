package com.dashboard.core.communication

/**
 * The wire-level contract between phone and dashboard. Every message here is transport-agnostic
 * — nothing in this file knows Bluetooth exists. [BluetoothProvider][com.dashboard.core.hardware.BluetoothProvider]
 * only ever carries the [ByteArray] that [MessageCodec] produces from/into these types.
 *
 * Fields are simple (nullable primitives/strings/enums-as-strings) so [MessageCodec] can encode
 * them without needing a JSON/protobuf library (none is reachable from this sandbox's network,
 * see README). When this project moves to Android Studio with real Maven access, swapping
 * [MessageCodec]'s body for kotlinx.serialization is a self-contained change — nothing outside
 * `communication/` needs to know.
 */
sealed class ProtocolMessage {

    data class NavigationUpdate(
        val active: Boolean,
        val direction: String, // Direction.name
        val distanceMeters: Double?,
        val roadName: String?,
        val etaMinutes: Int?,
    ) : ProtocolMessage()

    data class MediaUpdate(
        val title: String?,
        val artist: String?,
        val album: String?,
        val playbackState: String, // PlaybackState.name
        val positionMillis: Long,
        val durationMillis: Long,
    ) : ProtocolMessage()

    data class MediaCommandMessage(
        val command: String, // MediaCommand.name
    ) : ProtocolMessage()

    data class BlizzerTrigger(
        val id: String,
        val type: String, // BlizzerEventType.name
        val message: String,
        val timestampMillis: Long,
        val active: Boolean,
        val distanceMeters: Int?,
    ) : ProtocolMessage()

    data class ConnectionUpdate(
        val state: String, // ConnectionState.name
    ) : ProtocolMessage()

    /** Placeholder shape for future settings sync; payload format is finalized in the settings step. */
    data class SettingsUpdate(
        val payload: String,
    ) : ProtocolMessage()
}
