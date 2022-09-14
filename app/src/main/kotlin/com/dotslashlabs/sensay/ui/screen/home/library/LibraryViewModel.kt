package com.dotslashlabs.sensay.ui.screen.home.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.screen.home.SortFilter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import logcat.logcat

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
) : MavericksState

class LibraryViewModel @AssistedInject constructor(
    @Assisted private val state: LibraryState,
    private val store: SensayStore,
) : MavericksViewModel<LibraryState>(state) {

    init {
        onEach(LibraryState::sortFilter) { (sortType, isAscending) ->
            store.booksProgressWithBookAndChapters(
                listOf(BookCategory.NOT_STARTED, BookCategory.FINISHED),
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

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<LibraryViewModel, LibraryState> {
        override fun create(state: LibraryState): LibraryViewModel
    }

    companion object : MavericksViewModelFactory<LibraryViewModel, LibraryState>
    by hiltMavericksViewModelFactory()
}
