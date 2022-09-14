package com.dotslashlabs.sensay.ui.screen.home.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.dotslashlabs.sensay.ui.screen.home.BooksGrid
import com.dotslashlabs.sensay.ui.screen.home.BooksList
import com.dotslashlabs.sensay.util.isLifecycleResumed
import config.HomeLayout

typealias OnNavToBook = (bookId: Long) -> Unit

object LibraryScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
        val homeLayout by appViewModel.collectAsState(SensayAppState::homeLayout)

        val viewModel: LibraryViewModel = mavericksViewModel(backStackEntry)
        val state by viewModel.collectAsState()

        val onNavToBook: OnNavToBook = { bookId ->
            if (backStackEntry.isLifecycleResumed()) {
                navHostController.navigate(Destination.Player.useRoute(bookId))
            }
        }

        SensayFrame {
            when (homeLayout) {
                HomeLayout.LIST -> BooksList(
                    state.books,
                    state.sortMenuItems,
                    state.sortFilter,
                    onSortMenuChange = viewModel::setSortFilter,
                    onNavToBook = onNavToBook,
                )
                HomeLayout.GRID -> BooksGrid(
                    state.books,
                    state.sortMenuItems,
                    state.sortFilter,
                    onSortMenuChange = viewModel::setSortFilter,
                    onNavToBook = onNavToBook,
                )
            }
        }
    }
}
