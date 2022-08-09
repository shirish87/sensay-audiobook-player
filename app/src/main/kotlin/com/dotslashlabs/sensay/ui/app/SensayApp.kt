package com.dotslashlabs.sensay.ui.app

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import logcat.logcat


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SensayApp(
    activityBridge: ActivityBridge,
) {
    val viewModel: SensayAppViewModel = mavericksViewModel()
    val state by viewModel.collectAsState()
    logcat("SensayApp") { "appState=$state" }

    val navController = rememberAnimatedNavController()
    val destination = Destination.Root

    SensayTheme {
        AnimatedNavHost(
            navController = navController,
            startDestination = destination.defaultChild.route,
        ) {
            destination.children.map {
                it.screen?.navGraph(it, this, navController, activityBridge)
            }
        }
    }
}
