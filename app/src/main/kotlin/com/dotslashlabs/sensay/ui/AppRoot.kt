package com.dotslashlabs.sensay.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewModel
import com.dotslashlabs.sensay.ui.nav.AppNavTree
import logcat.logcat

@Composable
fun AppRoot() {
    AppNavTree()

    val nowPlayingViewModel: NowPlayingViewModel = mavericksActivityViewModel()

    LaunchedEffect(nowPlayingViewModel) {
        logcat("AppRoot") { "nowPlayingViewModel: ${nowPlayingViewModel.awaitState()}"}
    }
}
