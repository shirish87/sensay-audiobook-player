package com.dotslashlabs.sensay.ui.screen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.composable

interface SensayScreen {

    @OptIn(ExperimentalAnimationApi::class)
    fun navGraph(
        destination: Destination,
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
    ) = navGraphBuilder.composable(
        route = destination.route,
        arguments = destination.arguments,
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
