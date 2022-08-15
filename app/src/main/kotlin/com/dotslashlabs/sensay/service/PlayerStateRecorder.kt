package com.dotslashlabs.sensay.service

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PlayerStateRecorder<T>(
    private val interval: Duration = 10.seconds,
    private val playerProvider: () -> Player?,
    private val stateFlow: MutableStateFlow<T>,
    private val newStateFromPlayer: (Player) -> T,
) {
    private var stateRecorderJob: Job? = null

    private val playerRef: Player?
        get() = playerProvider()


    fun recordState(player: Player? = playerRef) {
        if (player == null || player.currentPosition == C.TIME_UNSET) return

        stateFlow.value = newStateFromPlayer(player)
    }

    fun startStateRecorder(scope: CoroutineScope) {
        stopStateRecorder()

        stateRecorderJob = scope.launch {
            val delayInMillis = interval.inWholeMilliseconds

            while (true) {
                recordState()
                delay(delayInMillis)
            }
        }
    }

    fun stopStateRecorder() {
        stateRecorderJob?.cancel()
        stateRecorderJob = null
    }

    fun release() {
        stopStateRecorder()
    }
}
