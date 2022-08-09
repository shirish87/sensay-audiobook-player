package com.dotslashlabs.sensay.ui.app

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

data class SensayAppState(
    val data: String = "",
) : MavericksState

class SensayAppViewModel @AssistedInject constructor(
    @Assisted private val state: SensayAppState,
) : MavericksViewModel<SensayAppState>(state) {

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<SensayAppViewModel, SensayAppState> {
        override fun create(state: SensayAppState): SensayAppViewModel
    }

    companion object : MavericksViewModelFactory<SensayAppViewModel, SensayAppState>
    by hiltMavericksViewModelFactory()
}
