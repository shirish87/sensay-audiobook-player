package com.dotslashlabs.sensay.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import com.dotslashlabs.sensay.util.DevicePosture
import com.dotslashlabs.sensay.util.toWindowSizeClass
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SensayApp(
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
) {

    val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
    appViewModel.configure(windowSize.toWindowSizeClass(), devicePosture)

    val navController = rememberAnimatedNavController()
    val destination = Destination.Root

    SensayTheme {
        AnimatedNavHost(
            navController = navController,
            startDestination = destination.defaultChild.route,
        ) {
            destination.children.map {
                it.screen?.navGraph(it, this, navController)
            }
        }
    }
}
