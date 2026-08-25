package com.dashboard.core.communication

/**
 * Encodes/decodes [ProtocolMessage]s to/from [ByteArray].
 *
 * Wire format is a deliberately simple, dependency-free line format:
 * ```
 * TYPE_NAME
 * key=value
 * key=value
 * ...
 * ```
 * `null` values are encoded as the literal string `\u0000null` so they're distinguishable from
 * a real empty string. This is NOT meant to be the final production format — it exists so the
 * Communication Layer can be built and tested today without a JSON/protobuf dependency (this
 * sandbox has no Maven access, see README). Everything above this file (BluetoothPhoneCommunication,
 * MockPhoneCommunication, and every panel) depends only on [ProtocolMessage] and [encode]/[decode],
 * so swapping the wire format later is fully contained here.
 */
object MessageCodec {

    private const val NULL_MARKER = "\u0000null"

    fun encode(message: ProtocolMessage): ByteArray {
        val lines = mutableListOf<String>()
        when (message) {
            is ProtocolMessage.NavigationUpdate -> {
                lines += "NavigationUpdate"
                lines += "active=${message.active}"
                lines += "direction=${message.direction}"
                lines += "distanceMeters=${message.distanceMeters.encodeNullable()}"
                lines += "roadName=${message.roadName.encodeNullable()}"
                lines += "etaMinutes=${message.etaMinutes.encodeNullable()}"
            }
            is ProtocolMessage.MediaUpdate -> {
                lines += "MediaUpdate"
                lines += "title=${message.title.encodeNullable()}"
                lines += "artist=${message.artist.encodeNullable()}"
                lines += "album=${message.album.encodeNullable()}"
                lines += "playbackState=${message.playbackState}"
                lines += "positionMillis=${message.positionMillis}"
                lines += "durationMillis=${message.durationMillis}"
            }
            is ProtocolMessage.MediaCommandMessage -> {
                lines += "MediaCommandMessage"
                lines += "command=${message.command}"
            }
            is ProtocolMessage.BlizzerTrigger -> {
                lines += "BlizzerTrigger"
                lines += "id=${message.id}"
                lines += "type=${message.type}"
                lines += "message=${message.message}"
                lines += "timestampMillis=${message.timestampMillis}"
                lines += "active=${message.active}"
                lines += "distanceMeters=${message.distanceMeters.encodeNullable()}"
            }
            is ProtocolMessage.ConnectionUpdate -> {
                lines += "ConnectionUpdate"
                lines += "state=${message.state}"
            }
            is ProtocolMessage.SettingsUpdate -> {
                lines += "SettingsUpdate"
                lines += "payload=${message.payload}"
            }
        }
        return lines.joinToString("\n").toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): ProtocolMessage {
        val lines = String(bytes, Charsets.UTF_8).split("\n")
        require(lines.isNotEmpty()) { "empty message" }
        val type = lines[0]
        val fields = lines.drop(1).associate { line ->
            val idx = line.indexOf('=')
            require(idx >= 0) { "malformed field line: $line" }
            line.substring(0, idx) to line.substring(idx + 1)
        }

        fun field(name: String) = fields[name] ?: error("missing field '$name' in $type message")

        return when (type) {
            "NavigationUpdate" -> ProtocolMessage.NavigationUpdate(
                active = field("active").toBoolean(),
                direction = field("direction"),
                distanceMeters = field("distanceMeters").decodeNullableDouble(),
                roadName = field("roadName").decodeNullableString(),
                etaMinutes = field("etaMinutes").decodeNullableInt(),
            )
            "MediaUpdate" -> ProtocolMessage.MediaUpdate(
                title = field("title").decodeNullableString(),
                artist = field("artist").decodeNullableString(),
                album = field("album").decodeNullableString(),
                playbackState = field("playbackState"),
                positionMillis = field("positionMillis").toLong(),
                durationMillis = field("durationMillis").toLong(),
            )
            "MediaCommandMessage" -> ProtocolMessage.MediaCommandMessage(command = field("command"))
            "BlizzerTrigger" -> ProtocolMessage.BlizzerTrigger(
                id = field("id"),
                type = field("type"),
                message = field("message"),
                timestampMillis = field("timestampMillis").toLong(),
                active = field("active").toBoolean(),
                distanceMeters = field("distanceMeters").decodeNullableInt(),
            )
            "ConnectionUpdate" -> ProtocolMessage.ConnectionUpdate(state = field("state"))
            "SettingsUpdate" -> ProtocolMessage.SettingsUpdate(payload = field("payload"))
            else -> error("unknown message type: $type")
        }
    }

    private fun Any?.encodeNullable(): String = this?.toString() ?: NULL_MARKER
    private fun String.decodeNullableString(): String? = if (this == NULL_MARKER) null else this
    private fun String.decodeNullableDouble(): Double? = if (this == NULL_MARKER) null else toDouble()
    private fun String.decodeNullableInt(): Int? = if (this == NULL_MARKER) null else toInt()
}
