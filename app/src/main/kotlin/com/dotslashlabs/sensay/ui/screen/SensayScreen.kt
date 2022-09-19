package com.dotslashlabs.sensay.ui.screen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.composable

interface SensayScreen {

    companion object {
        private const val APP_NAME = "sensay"

        fun getUriString(path: String) = "content://$APP_NAME/$path"
    }

    @OptIn(ExperimentalAnimationApi::class)
    fun navGraph(
        destination: Destination,
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
    ) = navGraphBuilder.composable(
        route = destination.route,
        arguments = destination.arguments,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = getUriString(destination.route)
            }
        ),
    ) {
        Content(
            destination,
            navHostController,
            it,
        )
    }

    @Composable
    fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    )
}
