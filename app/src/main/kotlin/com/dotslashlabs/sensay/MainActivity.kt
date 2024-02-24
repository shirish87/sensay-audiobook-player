package com.dotslashlabs.sensay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.dotslashlabs.sensay.ui.AppRoot
import com.dotslashlabs.sensay.ui.common.LocalWindowSize
import com.dotslashlabs.sensay.ui.common.WindowSize
import com.dotslashlabs.sensay.ui.theme.SensayTheme
import dagger.hilt.android.AndroidEntryPoint
import media.BindConnection
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // This app draws behind the system bars, so we want to handle fitting system windows
//        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(activity = this)
            val windowSize = WindowSize(windowSizeClass.widthSizeClass, windowSizeClass.heightSizeClass)

            CompositionLocalProvider(LocalWindowSize provides windowSize) {
                SensayTheme {
                    AppRoot()
                }
            }
        }
    }
}
