package com.dotslashlabs.sensay.ui.screen.home

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

enum class Layout {
    LIST,
    GRID,
}

data class HomeViewState(
    @PersistState val activeLayout: Layout = Layout.LIST,
) : MavericksState

class HomeViewModel @AssistedInject constructor(
    @Assisted private val state: HomeViewState,
) : MavericksViewModel<HomeViewState>(state) {

    fun setActiveLayout(layout: Layout) {
        setState { copy(activeLayout = layout) }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState>
    by hiltMavericksViewModelFactory()
}
