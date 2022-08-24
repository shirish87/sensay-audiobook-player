package com.dotslashlabs.sensay.common

import com.dotslashlabs.sensay.util.PlayerState

data class PlaybackConnectionState(
    val isConnected: Boolean = false,
    val playerState: PlayerState = PlayerState(),
    val playerMediaIds: List<String> = emptyList(),
)
