package com.dotslashlabs.sensay.ui.screen.home

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.BookProgressUpdate
import data.BookProgressVisibility
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class HomeSortType(
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

data class HomeViewState(
    @PersistState val bookCategories: Collection<BookCategory>,
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
    val progressRestorableCount: Async<Int> = Uninitialized,

    val sortMenuItems: Collection<Pair<HomeSortType, ImageVector>> = HomeSortType.values()
        .map { it to it.imageVector },

    val sortFilter: SortFilter<HomeSortType> = HomeSortType.UPDATED to false,

    val isFilterEnabled: Boolean = false,
    val filter: String = "",

    val isAuthorFilterEnabled: Boolean = false,
    val authorsFilter: List<String> = emptyList(),
) : MavericksState {

    constructor(args: HomeViewArgs) : this(bookCategories = args.bookCategories)

    val authors: List<String> = books()?.fold(mutableSetOf<String>()) { acc, b ->
        b.book.author?.let { acc.add(it) }
        acc
    }?.sorted() ?: emptyList()
}

class HomeViewModel @AssistedInject constructor(
    @Assisted state: HomeViewState,
    private val store: SensayStore,
) : HomeBaseViewModel<HomeViewState>(state) {

    private val bookCategories = state.bookCategories

    init {
        onEachThrottled(
            HomeViewState::sortFilter,
            HomeViewState::authorsFilter,
            HomeViewState::filter,
            delayByMillis = { _, _, filter -> if (filter.length > 3) 200L else 50L },
        ) { (sortType, isAscending), authorsFilter, filter ->

            val filterCondition = if (filter.isNotBlank()) "%${filter.lowercase()}%" else "%"

            store.booksProgressWithBookAndChapters(
                bookCategories,
                filter = filterCondition,
                authorsFilter = authorsFilter,
                orderBy = sortType.columnName,
                isAscending = isAscending,
            ).execute(retainValue = HomeViewState::books) {
                copy(books = it)
            }
        }

        store.progressRestorableCount()
            .execute(retainValue = HomeViewState::progressRestorableCount) {
                copy(progressRestorableCount = it)
            }
    }

    fun setSortFilter(sortFilter: SortFilter<HomeSortType>) {
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

    fun setAuthorFilterEnabled(enabled: Boolean) {
        setState {
            if (enabled) {
                copy(isAuthorFilterEnabled = enabled)
            } else {
                copy(isAuthorFilterEnabled = enabled, authorsFilter = emptyList())
            }
        }
    }

    fun setAuthorsFilter(authorsFilter: List<String>) {
        setState { copy(authorsFilter = authorsFilter) }
    }

    fun setBookCategory(
        bookProgressWithChapters: BookProgressWithBookAndChapters,
        bookCategory: BookCategory,
    ) = viewModelScope.launch(Dispatchers.IO) {

        val chapters = bookProgressWithChapters.chapters.sortedBy { it.trackId }

        val chapter = chapters.run {
            if (bookCategory == BookCategory.FINISHED) {
                last()
            } else {
                first()
            }
        }

        val bookProgress = if (bookCategory == BookCategory.FINISHED)
            bookProgressWithChapters.book.duration
        else chapter.start

        val update = BookProgressUpdate(
            bookProgressId = bookProgressWithChapters.bookProgress.bookProgressId,
            bookCategory = bookCategory,
            chapterId = chapter.chapterId,
            currentChapter = if (bookCategory == BookCategory.FINISHED)
                bookProgressWithChapters.bookProgress.totalChapters else 0,
            chapterProgress = if (bookCategory == BookCategory.FINISHED)
                chapter.duration else ContentDuration.ZERO,
            chapterTitle = chapter.title,
            bookProgress = bookProgress,
            bookRemaining = ContentDuration.ms(
                bookProgressWithChapters.durationMs - bookProgress.ms
            ),
        )

        store.updateBookProgress(update)
    }

    fun setBookVisibility(
        bookProgressWithChapters: BookProgressWithBookAndChapters,
        isVisible: Boolean,
    ) = viewModelScope.launch(Dispatchers.IO) {

        store.updateBookProgress(
            BookProgressVisibility(
                bookProgressId = bookProgressWithChapters.bookProgress.bookProgressId,
                isVisible = isVisible,
            )
        )
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState>
    by hiltMavericksViewModelFactory()
}

data class HomeViewArgs(
    val bookCategories: Collection<BookCategory>,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.createStringArray()!!.map { BookCategory.valueOf(it) },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringArray(bookCategories.map { it.name }.toTypedArray())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HomeViewArgs> {
        override fun createFromParcel(parcel: Parcel): HomeViewArgs {
            return HomeViewArgs(parcel)
        }

        override fun newArray(size: Int): Array<HomeViewArgs?> {
            return arrayOfNulls(size)
        }
    }
}
