package com.dotslashlabs.sensay.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerHolder {

    private val stateFlow = MutableStateFlow<SensayPlayer?>(null)
    val connection = stateFlow.asStateFlow()

    fun load(player: SensayPlayer?) {
        stateFlow.value = player
    }

    fun clear() {
        stateFlow.apply {
            value?.release()
            value = null
        }
    }
}
