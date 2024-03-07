package com.dotslashlabs.sensay.ui

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewModel
import com.dotslashlabs.sensay.ui.nav.AppNavTree
import logcat.logcat

@Composable
fun AppRoot(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    context: Context = LocalContext.current,
) {
    AppNavTree()

    val nowPlayingViewModel: NowPlayingViewModel = mavericksActivityViewModel()

    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            when {
                (event == Lifecycle.Event.ON_START) -> {
                    nowPlayingViewModel.initialize(context)
                }
                (event == Lifecycle.Event.ON_STOP) -> {
                    nowPlayingViewModel.release()
                }
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}
