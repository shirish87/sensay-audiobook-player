package com.dotslashlabs.sensay.ui.screen.home.current

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.SensayAppState
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.BooksGrid
import com.dotslashlabs.sensay.ui.screen.home.BooksList
import config.HomeLayout

object CurrentScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
        val homeLayout by appViewModel.collectAsState(SensayAppState::homeLayout)

        val viewModel: CurrentViewModel = mavericksViewModel(backStackEntry)
        val state by viewModel.collectAsState()

        val onNavToBook = { bookId: Long ->
            navHostController.navigate(Destination.Book.Player.useRoute(bookId))
        }

        SensayFrame {
            when (homeLayout) {
                HomeLayout.LIST -> BooksList(state.books, onNavToBook = onNavToBook)
                HomeLayout.GRID -> BooksGrid(state.books, onNavToBook = onNavToBook)
            }
        }
    }
}
