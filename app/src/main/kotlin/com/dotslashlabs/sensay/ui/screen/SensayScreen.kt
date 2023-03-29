package com.dotslashlabs.sensay.ui.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink

interface SensayScreen {

    companion object {
        private const val APP_NAME = "sensay"

        fun getUriString(path: String) = "content://$APP_NAME/$path"
    }

    fun navGraph(
        destination: Destination,
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
    ) = if (destination.isDialog) {
        navGraphBuilder.dialog(
            route = destination.route,
            arguments = destination.arguments,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = getUriString(destination.route)
                },
            ),
        ) {
            Content(
                destination,
                navHostController,
                it,
            )
        }
    } else {
        navGraphBuilder.composable(
            route = destination.route,
            arguments = destination.arguments,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = getUriString(destination.route)
                },
            ),
        ) {
            Content(
                destination,
                navHostController,
                it,
            )
        }
    }

    @Composable
    fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    )
}
