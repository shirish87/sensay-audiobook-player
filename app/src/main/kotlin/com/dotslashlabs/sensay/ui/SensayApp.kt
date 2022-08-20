package com.dotslashlabs.sensay.ui

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import com.dotslashlabs.sensay.util.DevicePosture
import com.dotslashlabs.sensay.util.toWindowSizeClass
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import logcat.logcat


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SensayApp(
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
    intentAction: String?,
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

    LaunchedEffect(Unit) {
        if (intentAction == Intent.ACTION_VIEW) {
            val lastPlayedBookId = appViewModel.getLastPlayedBookId() ?: return@LaunchedEffect
            if (lastPlayedBookId == -1L) return@LaunchedEffect

            val deepLink = SensayScreen.getUriString(
                Destination.Player.useRoute(lastPlayedBookId)
            ).toUri()

            logcat { "LaunchedEffect.navigate(deepLink=$deepLink)" }
            navController.navigate(deepLink)
        }
    }
}
