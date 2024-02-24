package com.dotslashlabs.sensay.ui.common

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf

data class WindowSize(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) {

    val isLandscape =
        (widthSizeClass == WindowWidthSizeClass.Expanded &&
                heightSizeClass == WindowHeightSizeClass.Compact)

    val isPortrait =
        (widthSizeClass == WindowWidthSizeClass.Compact &&
                heightSizeClass == WindowHeightSizeClass.Expanded)
}

val LocalWindowSize = compositionLocalOf<WindowSize> { error("No window size found!") }
