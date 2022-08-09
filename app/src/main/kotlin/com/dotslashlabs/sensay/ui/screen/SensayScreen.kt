package com.dotslashlabs.sensay.ui.screen

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.dotslashlabs.sensay.ActivityBridge
import com.google.accompanist.navigation.animation.composable

interface SensayScreen {

    @OptIn(ExperimentalAnimationApi::class)
    fun navGraph(
        destination: Destination,
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
        activityBridge: ActivityBridge,
    ) = navGraphBuilder.composable(
        route = destination.route,
        arguments = destination.arguments,
    ) {
        content(
            destination,
            activityBridge,
            navHostController,
            it,
        )
    }

    @Composable
    fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    )
}
