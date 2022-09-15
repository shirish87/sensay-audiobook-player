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
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters


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

    val sortMenuItems: Collection<Pair<HomeSortType, ImageVector>> = HomeSortType.values()
        .map { it to it.imageVector },

    val sortFilter: SortFilter<HomeSortType> = HomeSortType.UPDATED to false,

    val isFilterEnabled: Boolean = false,
    val filter: String = "",
) : MavericksState {

    constructor(args: HomeViewArgs) : this(bookCategories = args.bookCategories)
}


class HomeViewModel @AssistedInject constructor(
    @Assisted state: HomeViewState,
    private val store: SensayStore,
) : HomeBaseViewModel<HomeViewState>(state) {

    private val bookCategories = state.bookCategories

    init {
        onEachThrottled(
            HomeViewState::sortFilter,
            HomeViewState::filter,
            delayByMillis = { _, filter -> if (filter.length > 1) 200L else 0L },
        ) { (sortType, isAscending), filter ->

            val filterCondition = if (filter.isNotBlank()) "%${filter.lowercase()}%" else "%"

            store.booksProgressWithBookAndChapters(
                bookCategories, // listOf(BookCategory.NOT_STARTED, BookCategory.FINISHED),
                filter = filterCondition,
                orderBy = sortType.columnName,
                isAscending = isAscending,
            ).execute(retainValue = HomeViewState::books) {
                copy(books = it)
            }
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
