package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ui.SensayAppState
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.util.isLifecycleResumed
import config.HomeLayout
import data.BookCategory
import data.entity.BookProgressWithBookAndChapters

object CurrentScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
        val homeLayout by appViewModel.collectAsState(SensayAppState::homeLayout)

        val viewModel: HomeViewModel = mavericksViewModel(backStackEntry, argsFactory = {
            HomeViewArgs(listOf(BookCategory.CURRENT))
        })

        val state by viewModel.collectAsState()

        val onNavToBook: OnNavToBook = { bookId ->
            if (backStackEntry.isLifecycleResumed()) {
                navHostController.navigate(Destination.Player.useRoute(bookId))
            }
        }

        val sortMenuOptions = SortMenuOptions(
            state.sortMenuItems,
            state.sortFilter,
            onSortMenuChange = viewModel::setSortFilter,
        )

        val filterMenuOptions = FilterMenuOptions(
            isFilterEnabled = state.isFilterEnabled,
            onFilterEnabled = viewModel::setFilterEnabled,
            filter = state.filter,
            onFilterChange = viewModel::setFilter,
            filterLabel = "Book Title or Author",
        )

        val filterListOptions = FilterListOptions(
            isFilterEnabled = state.isAuthorFilterEnabled,
            onFilterEnabled = viewModel::setAuthorFilterEnabled,
            items = state.authors,
            selection = state.authorsFilter,
            filterLabel = "Authors",
            onAdd = {
                viewModel.setAuthorsFilter((state.authorsFilter + it).sorted())
            },
            onDelete = {
                viewModel.setAuthorsFilter((state.authorsFilter - it).sorted())
            },
        )

        val bookContextMenuConfig = BookContextMenuConfig(
            isRestoreBookEnabled = (state.progressRestorableCount() ?: 0) > 0,
            isVisibilityChangeEnabled = false,
            onNavToRestore = { bookId ->
                if (backStackEntry.isLifecycleResumed()) {
                    navHostController.navigate(Destination.Restore.useRoute(bookId))
                }
            },
            onSetBookCategory = viewModel::setBookCategory,
            onBookVisibilityChange = viewModel::setBookVisibility,
        )

        val context = LocalContext.current
        var isPlayerAvailable by remember { mutableStateOf(false) }

        val onPlay: ((bookProgressWithChapters: BookProgressWithBookAndChapters) -> Unit)? =
            if (isPlayerAvailable) viewModel::play else null

        DisposableEffect(viewModel, context) {
            viewModel.attachPlayer(context) { err ->
                isPlayerAvailable = (err == null)
            }

            onDispose {
                isPlayerAvailable = false
                viewModel.detachPlayer()
            }
        }

        SensayFrame {
            when (homeLayout) {
                HomeLayout.LIST -> BooksList(
                    state.books,
                    bookContextMenuConfig,
                    sortMenuOptions,
                    filterMenuOptions,
                    filterListOptions,
                    onNavToBook = onNavToBook,
                    onPlay = onPlay,
                )
                HomeLayout.GRID -> BooksGrid(
                    state.books,
                    bookContextMenuConfig,
                    sortMenuOptions,
                    filterMenuOptions,
                    filterListOptions,
                    onNavToBook = onNavToBook,
                    onPlay = onPlay,
                )
            }
        }
    }
}
