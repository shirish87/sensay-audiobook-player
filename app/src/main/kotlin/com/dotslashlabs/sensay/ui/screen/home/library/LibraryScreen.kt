package com.dotslashlabs.sensay.ui.screen.home.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.BooksGrid
import com.dotslashlabs.sensay.ui.screen.home.BooksList
import config.HomeLayout

object LibraryScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {
        val viewModel: LibraryViewModel = mavericksViewModel(backStackEntry)
        val state by viewModel.collectAsState()

        val onNavToBook = { bookId: Long ->
            navHostController.navigate(Destination.Book.Player.useRoute(bookId))
        }

        val items = when (state.books) {
            is Success -> state.books() ?: emptyList()
            else -> emptyList()
        }

        SensayFrame {
            when (state.homeLayout) {
                HomeLayout.LIST -> BooksList(items, onNavToBook = onNavToBook)
                HomeLayout.GRID -> BooksGrid(items, onNavToBook = onNavToBook)
            }
        }
    }
}
