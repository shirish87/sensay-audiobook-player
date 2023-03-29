package com.dotslashlabs.sensay.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import com.dotslashlabs.sensay.util.DevicePosture
import com.dotslashlabs.sensay.util.toWindowSizeClass
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import logcat.logcat

@Composable
fun SensayApp(
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
    navToLastBook: Boolean,
) {

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()

    val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
    appViewModel.configure(windowSize.toWindowSizeClass(), devicePosture)

    val navController = rememberNavController()
    val destination = Destination.Root

    SensayTheme {
        NavHost(
            navController = navController,
            startDestination = destination.defaultChild.route,
        ) {
            destination.children.map {
                it.screen?.navGraph(it, this, navController)
            }
        }
    }

    LaunchedEffect(navToLastBook) {
        if (navToLastBook) {
            val lastPlayedBookId = appViewModel.getLastPlayedBookId() ?: return@LaunchedEffect
            if (lastPlayedBookId == -1L) return@LaunchedEffect

            val deepLink = SensayScreen.getUriString(
                Destination.Player.useRoute(lastPlayedBookId)
            ).toUri()

            logcat { "LaunchedEffect.navigate(deepLink=$deepLink)" }
            navController.navigate(deepLink)
        }
    }

    DisposableEffect(systemUiController, useDarkIcons) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )

        onDispose {}
    }

    val playerAppViewModel: PlayerAppViewModel = mavericksActivityViewModel()
    val context = LocalContext.current

    DisposableEffect(playerAppViewModel, context) {
        logcat { "SensayApp.attachPlayer" }
        playerAppViewModel.attachPlayer(context)

        onDispose {
            logcat { "SensayApp.detachPlayer" }
            playerAppViewModel.detachPlayer()
        }
    }
}
