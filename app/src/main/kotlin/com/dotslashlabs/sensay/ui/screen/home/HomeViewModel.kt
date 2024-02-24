package com.dotslashlabs.sensay.ui.screen.home

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import compose.icons.MaterialIcons
import compose.icons.materialicons.HourglassTop
import compose.icons.materialicons.Person
import compose.icons.materialicons.Schedule
import compose.icons.materialicons.Timer
import compose.icons.materialicons.Title
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.logcat

data class ListItem(
    val isDirectory: Boolean,
    val path: String,
    val fileUri: Uri? = null,
    val label: String = "",
    val children: List<ListItem> = emptyList(),
)

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

enum class HomeSortType(
    private val displayName: String,
    val imageVector: ImageVector,
    val columnName: String,
) {

    TITLE("Title", MaterialIcons.Title, "bookTitle"),
    AUTHOR("Author", MaterialIcons.Person, "bookAuthor"),
    UPDATED("Updated", MaterialIcons.Timer, "lastUpdatedAt"),
    REMAINING("Remaining", MaterialIcons.HourglassTop, "bookRemaining"),
    ADDED("Added", MaterialIcons.Schedule, "createdAt"),
    ;

    override fun toString() = displayName
}

data class HomeViewState(
    val location: Async<String> = Uninitialized,
    val items: Async<Pair<String, List<ListItem>>> = Uninitialized,
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,

    val isSearchEnabled: Boolean = false,
    val search: String = "",

    val isFilterListEnabled: Boolean = false,
    val isFilterListVisible: Boolean = false,
    val filterListItems: Async<List<String>> = Uninitialized,
    val filterListSelections: Async<List<String>> = Uninitialized,

    val sortFilter: SortFilter<HomeSortType> = HomeSortType.UPDATED to false,
) : MavericksState {

    val isBackEnabled = (location()?.count { it == '/' } ?: 0) > 1

    val isHomeLayoutGrid = homeLayout == HomeLayout.GRID

    val sortMenuOptions = SortMenuOptions(
        sortMenuItems = HomeSortType.values().map { it to it.imageVector },
        sortMenuDefaults = HomeSortType.UPDATED to false,
    )

    val filterMenuOptions = FilterMenuOptions(
        isFilterEnabled = isSearchEnabled,
        filter = search,
        filterLabel = "Book Title or Author",
    )

    val filterListOptions = FilterListOptions(
        isFilterEnabled = isFilterListEnabled,
        isFilterVisible = isFilterListVisible,
        items = filterListItems.invoke() ?: emptyList(),
        selection = filterListSelections.invoke() ?: emptyList(),
        filterLabel = "Authors",
    )
}

class HomeViewModel @AssistedInject constructor(
    @Assisted initialState: HomeViewState,
    private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<HomeViewState>(initialState) {

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it() ?: this.homeLayout)
        }

        onAsync(HomeViewState::location, onFail = { err ->
            setState { copy(items = Fail(err)) }
        }, ::loadLocation)

        onEach(
            HomeViewState::sortFilter,
            HomeViewState::isSearchEnabled,
            HomeViewState::isFilterListEnabled,
            HomeViewState::filterListSelections,
        ) { _, _, _, _ -> loadLocation("/") }

        onEach(
            HomeViewState::search,
        ) { _ ->
            delay(200)
            loadLocation("/")
        }

        onAsync(HomeViewState::books, onFail = { err ->
            setState { copy(items = Fail(err)) }
        }, {
            setState {
                copy(
                    filterListItems = Success(
                        it.mapNotNull { b -> b.book.author }.distinct().sorted()
                    ),
                )
            }
        })

        viewModelScope.launch {
            setLocation("/")
        }
    }

    fun setFilterListVisible(visible: Boolean) = setState {
        copy(isFilterListVisible = visible)
    }

    fun setFilterListEnabled(enabled: Boolean) = setState {
        copy(
            isFilterListEnabled = enabled,
            isFilterListVisible = enabled,
            filterListSelections = if (enabled) filterListSelections else Success(emptyList()),
        )
    }

    fun setSortFilter(sortFilter: SortFilter<HomeSortType>) = setState {
        copy(sortFilter = sortFilter)
    }

    fun addFilterListSelection(selection: String) = setState {
        copy(
            filterListSelections = Success(
                (filterListSelections.invoke() ?: emptyList()) + selection
            )
        )
    }

    fun deleteFilterListSelection(selection: String) = setState {
        copy(
            filterListSelections = Success(
                (filterListSelections.invoke() ?: emptyList()) - selection
            )
        )
    }

    fun setSearchEnabled(enabled: Boolean) = setState {
        copy(isSearchEnabled = enabled, search = if (enabled) search else "")
    }

    fun setSearch(search: String) = setState {
        copy(search = search)
    }

    fun setHomeLayout(layout: HomeLayout) = viewModelScope.launch {
        configStore.setHomeLayout(layout)
    }

    private fun loadLocation(location: String) = viewModelScope.async {
        logcat { "loadLocation: $location" }
        setState { copy(books = Loading()) }

        val state = awaitState()
        val filterCondition = if (state.search.isNotBlank()) "%${state.search.lowercase()}%" else "%"

        store.booksProgressWithBookAndChapters(
            bookCategories = listOf(BookCategory.NOT_STARTED, BookCategory.CURRENT, BookCategory.FINISHED),
            filter = filterCondition,
            authorsFilter = state.filterListSelections.invoke() ?: emptyList(),
            orderBy = state.sortFilter.first.columnName,
            isAscending = state.sortFilter.second,
        )
        .execute(retainValue = HomeViewState::books) {
            copy(books = it)
        }.join()

        Pair(location, emptyList<ListItem>())
    }.execute(retainValue = HomeViewState::items) {
        copy(items = it)
    }

    fun reloadLocation() = withState {
        loadLocation(it.location() ?: "/")
    }

    fun setLocation(location: String) = if (location.endsWith("/"))
        setState {
            logcat { "setLocation: $location" }
            copy(location = Success(location))
        } else Unit

    fun setLocationToUp() = setState {
        copy(
            location = Success("/"),
        )
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState> by hiltMavericksViewModelFactory()
}
