package com.dotslashlabs.sensay.ui.screen.home.library

import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.DEFAULT_HOME_LAYOUT
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters

data class LibraryViewState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val booksCount: Int = 0,
) : MavericksState

class LibraryViewModel @AssistedInject constructor(
    @Assisted private val state: LibraryViewState,
    @Suppress("UNUSED_PARAMETER") private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<LibraryViewState>(state) {

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it.invoke() ?: this.homeLayout)
        }

        store.booksCount().execute {
            copy(booksCount = it.invoke() ?: this.booksCount)
        }

        store.booksProgressWithBookAndChapters().execute {
            copy(books = it)
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<LibraryViewModel, LibraryViewState> {
        override fun create(state: LibraryViewState): LibraryViewModel
    }

    companion object : MavericksViewModelFactory<LibraryViewModel, LibraryViewState>
    by hiltMavericksViewModelFactory()
}
