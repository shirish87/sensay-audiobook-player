package com.dotslashlabs.sensay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF51287A),
)

private val LightColorScheme = lightColorScheme()

@Composable
fun SensayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val layoutDirection = LocalLayoutDirection.current

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            Scaffold { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 0.dp,
                            bottom = 0.dp,
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        ),
                ) {
                    content()
                }
            }
        },
    )

//    LaunchedEffect(darkTheme) {
//        systemUiController.setSystemBarsColor(
//            color = Color.Transparent,
//            darkIcons = !darkTheme,
//        )
//    }
}

@Composable
fun SpacerSystemBarsTopPadding() {
    SpacerSystemBarsPadding(useTopPadding = true)
}

@Composable
fun SpacerSystemBarsBottomPadding() {
    SpacerSystemBarsPadding(useTopPadding = false)
}

@Composable
fun WithAppInsets(
    systemBarsPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues(),
    extraPadding: Dp = 6.dp,
    content: @Composable (insets: Pair<Dp, Dp>) -> Unit,
) {

    content(
        extraPadding + systemBarsPadding.calculateTopPadding() to
                extraPadding + systemBarsPadding.calculateBottomPadding(),
    )
}

@Composable
private fun SpacerSystemBarsPadding(
    useTopPadding: Boolean,
    systemBarsPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues(),
    extraPadding: Dp = 6.dp,
) {

    WithAppInsets(systemBarsPadding, extraPadding) { (top, bottom) ->
        Spacer(modifier = Modifier.height(if (useTopPadding) top else bottom))
    }
}
