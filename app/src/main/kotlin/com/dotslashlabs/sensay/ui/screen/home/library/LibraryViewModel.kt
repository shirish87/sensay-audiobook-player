package com.dotslashlabs.sensay.ui.screen.home.library

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


data class LibraryState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
) : MavericksState

class LibraryViewModel @AssistedInject constructor(
    @Assisted private val state: LibraryState,
    private val store: SensayStore,
) : MavericksViewModel<LibraryState>(state) {

    init {
        store.booksProgressWithBookAndChapters(
            listOf(BookCategory.NOT_STARTED, BookCategory.FINISHED),
        ).map {
            it.sortedBy { o -> -o.bookProgress.lastUpdatedAt.toEpochMilli() }
        }.execute {
            copy(books = it)
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<LibraryViewModel, LibraryState> {
        override fun create(state: LibraryState): LibraryViewModel
    }

    companion object : MavericksViewModelFactory<LibraryViewModel, LibraryState>
    by hiltMavericksViewModelFactory()
}
