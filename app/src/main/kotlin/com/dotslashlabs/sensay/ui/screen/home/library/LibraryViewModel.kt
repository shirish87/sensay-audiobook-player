package com.dotslashlabs.sensay.ui.screen.home.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.HomeBaseViewModel
import com.dotslashlabs.sensay.ui.screen.home.SortFilter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters

enum class LibrarySortType(
    private val displayName: String,
    val imageVector: ImageVector,
    val columnName: String,
) {
    TITLE("Title", Icons.Outlined.Title, "bookTitle"),
    AUTHOR("Author", Icons.Outlined.Person, "bookAuthor"),
    UPDATED("Updated", Icons.Outlined.Timer, "lastUpdatedAt"),
    REMAINING("Remaining", Icons.Outlined.HourglassTop, "bookRemaining"),
    ADDED("Added", Icons.Outlined.Schedule, "createdAt"),
    ;

    override fun toString() = displayName
}

data class LibraryState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,

    val sortMenuItems: Collection<Pair<LibrarySortType, ImageVector>> = LibrarySortType.values()
        .map { it to it.imageVector },

    val sortFilter: SortFilter<LibrarySortType> = LibrarySortType.UPDATED to false,

    val isFilterEnabled: Boolean = false,
    val filter: String = "",
) : MavericksState

class LibraryViewModel @AssistedInject constructor(
    @Assisted state: LibraryState,
    private val store: SensayStore,
) : HomeBaseViewModel<LibraryState>(state) {

    init {
        onEachThrottled(
            LibraryState::sortFilter,
            LibraryState::filter,
            delayByMillis = { _, filter -> if (filter.length > 1) 200L else 0L },
        ) { (sortType, isAscending), filter ->

            val filterCondition = if (filter.isNotBlank()) "%${filter.lowercase()}%" else "%"

            store.booksProgressWithBookAndChapters(
                listOf(BookCategory.NOT_STARTED, BookCategory.FINISHED),
                filter = filterCondition,
                orderBy = sortType.columnName,
                isAscending = isAscending,
            ).execute(retainValue = LibraryState::books) {
                copy(books = it)
            }
        }
    }

    fun setSortFilter(sortFilter: SortFilter<LibrarySortType>) {
        setState { copy(sortFilter = sortFilter) }
    }

    fun setFilterEnabled(enabled: Boolean) {
        setState {
            if (enabled) {
                copy(isFilterEnabled = enabled)
            } else {
                copy(isFilterEnabled = enabled, filter = "")
            }
        }
    }

    fun setFilter(filter: String) {
        setState { copy(filter = filter) }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<LibraryViewModel, LibraryState> {
        override fun create(state: LibraryState): LibraryViewModel
    }

    companion object : MavericksViewModelFactory<LibraryViewModel, LibraryState>
    by hiltMavericksViewModelFactory()
}
