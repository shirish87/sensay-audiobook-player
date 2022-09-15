package com.dotslashlabs.sensay.ui.screen.home.current

import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.HomeBaseViewModel
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

    val isFilterEnabled: Boolean = false,
    val filter: String = "",
) : MavericksState

class CurrentViewModel @AssistedInject constructor(
    @Assisted state: CurrentState,
    private val store: SensayStore,
) : HomeBaseViewModel<CurrentState>(state) {

    init {
        onEachThrottled(
            CurrentState::sortFilter,
            CurrentState::filter,
            delayByMillis = { _, filter -> if (filter.length > 1) 200L else 0L },
        ) { (sortType, isAscending), filter ->

            val filterCondition = if (filter.isNotBlank()) "%${filter.lowercase()}%" else "%"

            store.booksProgressWithBookAndChapters(
                listOf(BookCategory.CURRENT),
                filter = filterCondition,
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

    fun setFilterEnabled(enabled: Boolean) {
        setState {
            if (enabled) {
                copy(isFilterEnabled = true)
            } else {
                copy(isFilterEnabled = false, filter = "")
            }
        }
    }

    fun setFilter(filter: String) {
        setState { copy(filter = filter) }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<CurrentViewModel, CurrentState> {
        override fun create(state: CurrentState): CurrentViewModel
    }

    companion object : MavericksViewModelFactory<CurrentViewModel, CurrentState>
    by hiltMavericksViewModelFactory()
}
