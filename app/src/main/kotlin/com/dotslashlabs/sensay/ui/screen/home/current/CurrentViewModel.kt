package com.dotslashlabs.sensay.ui.screen.home.current

import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.SortFilter
import com.dotslashlabs.sensay.ui.screen.home.library.LibrarySortType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters

typealias CurrentSortType = LibrarySortType

data class CurrentState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,

    val sortMenuItems: Collection<Pair<CurrentSortType, ImageVector>> = CurrentSortType.values()
        .map { it to it.imageVector },

    val sortFilter: SortFilter<CurrentSortType> = CurrentSortType.UPDATED to false,
) : MavericksState

class CurrentViewModel @AssistedInject constructor(
    @Assisted private val state: CurrentState,
    private val store: SensayStore,
) : MavericksViewModel<CurrentState>(state) {

    init {
        onEach(CurrentState::sortFilter) { (sortType, isAscending) ->
            store.booksProgressWithBookAndChapters(
                listOf(BookCategory.CURRENT),
                orderBy = sortType.columnName,
                isAscending = isAscending,
            ).execute {
                copy(books = it)
            }
        }
    }

    fun setSortFilter(sortFilter: SortFilter<CurrentSortType>) {
        setState { copy(sortFilter = sortFilter) }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<CurrentViewModel, CurrentState> {
        override fun create(state: CurrentState): CurrentViewModel
    }

    companion object : MavericksViewModelFactory<CurrentViewModel, CurrentState>
    by hiltMavericksViewModelFactory()
}
