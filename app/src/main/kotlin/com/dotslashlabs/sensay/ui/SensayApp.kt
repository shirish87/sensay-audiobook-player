package com.dotslashlabs.sensay.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SensayApp(
    activityBridge: ActivityBridge,
) {
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
