package com.dotslashlabs.sensay.ui.screen.home.current

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.DEFAULT_HOME_LAYOUT
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore

data class CurrentViewState(
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val booksCount: Int = 0,
) : MavericksState

class CurrentViewModel @AssistedInject constructor(
    @Assisted private val state: CurrentViewState,
    @Suppress("UNUSED_PARAMETER") private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<CurrentViewState>(state) {

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it.invoke() ?: this.homeLayout)
        }

        store.booksCount().execute {
            copy(booksCount = it.invoke() ?: this.booksCount)
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<CurrentViewModel, CurrentViewState> {
        override fun create(state: CurrentViewState): CurrentViewModel
    }

    companion object : MavericksViewModelFactory<CurrentViewModel, CurrentViewState>
    by hiltMavericksViewModelFactory()
}
