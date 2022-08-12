package com.dotslashlabs.sensay.ui.screen.home.current

import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.flow.map


data class CurrentState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
) : MavericksState

class CurrentViewModel @AssistedInject constructor(
    @Assisted private val state: CurrentState,
    private val store: SensayStore,
) : MavericksViewModel<CurrentState>(state) {

    init {
        store.booksProgressWithBookAndChapters(
            listOf(BookCategory.CURRENT),
        ).map {
            it.sortedBy { o -> -o.bookProgress.lastUpdatedAt.toEpochMilli() }
        }.execute {
            copy(books = it)
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<CurrentViewModel, CurrentState> {
        override fun create(state: CurrentState): CurrentViewModel
    }

    companion object : MavericksViewModelFactory<CurrentViewModel, CurrentState>
    by hiltMavericksViewModelFactory()
}
