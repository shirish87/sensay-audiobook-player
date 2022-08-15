package com.dotslashlabs.sensay.ui.screen.book

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen

object BookScreen : SensayScreen {

    override fun navGraph(
        destination: Destination,
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
    ) {
        val startDestination = destination.defaultChild?.route ?: return

        navGraphBuilder.navigation(
            route = destination.route,
            startDestination = startDestination,
        ) {
            destination.children.map {
                it.screen?.navGraph(
                    it,
                    navGraphBuilder,
                    navHostController,
                )
            }
        }
    }

    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

    }
}
