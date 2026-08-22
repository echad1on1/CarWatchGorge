package com.dashboard.core.communication

import com.dashboard.core.domain.BlizzerEvent
import com.dashboard.core.domain.BlizzerEventType
import com.dashboard.core.domain.ConnectionState
import com.dashboard.core.domain.Direction
import com.dashboard.core.domain.MediaCommand
import com.dashboard.core.domain.MediaState
import com.dashboard.core.domain.NavigationState
import com.dashboard.core.domain.PlaybackState

/**
 * Translates between wire messages ([ProtocolMessage]) and the domain models the rest of the app
 * already uses (`NavigationState`, `MediaState`, etc.). Isolating this here means `domain/` stays
 * free of any notion of "message" or "wire format", and `communication/` stays the only place
 * that needs to change if a domain model gains a field.
 */

fun NavigationState.toProtocol() = ProtocolMessage.NavigationUpdate(
    active = active,
    direction = direction.name,
    distanceMeters = distanceMeters,
    roadName = roadName,
    etaMinutes = etaMinutes,
)

fun ProtocolMessage.NavigationUpdate.toDomain() = NavigationState(
    active = active,
    direction = runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.UNKNOWN),
    distanceMeters = distanceMeters,
    roadName = roadName,
    etaMinutes = etaMinutes,
)

fun MediaState.toProtocol() = ProtocolMessage.MediaUpdate(
    title = title,
    artist = artist,
    album = album,
    playbackState = playbackState.name,
    positionMillis = positionMillis,
    durationMillis = durationMillis,
)

fun ProtocolMessage.MediaUpdate.toDomain() = MediaState(
    title = title,
    artist = artist,
    album = album,
    playbackState = runCatching { PlaybackState.valueOf(playbackState) }.getOrDefault(PlaybackState.UNKNOWN),
    positionMillis = positionMillis,
    durationMillis = durationMillis,
)

fun MediaCommand.toProtocol() = ProtocolMessage.MediaCommandMessage(command = name)

fun ProtocolMessage.MediaCommandMessage.toDomain(): MediaCommand = MediaCommand.valueOf(command)

fun BlizzerEvent.toProtocol() = ProtocolMessage.BlizzerTrigger(
    id = id,
    type = type.name,
    message = message,
    timestampMillis = timestampMillis,
    active = active,
)

fun ProtocolMessage.BlizzerTrigger.toDomain() = BlizzerEvent(
    id = id,
    type = runCatching { BlizzerEventType.valueOf(type) }.getOrDefault(BlizzerEventType.INFO),
    message = message,
    timestampMillis = timestampMillis,
    active = active,
)

fun ConnectionState.toProtocol() = ProtocolMessage.ConnectionUpdate(state = name)

fun ProtocolMessage.ConnectionUpdate.toDomain(): ConnectionState =
    runCatching { ConnectionState.valueOf(state) }.getOrDefault(ConnectionState.ERROR)
