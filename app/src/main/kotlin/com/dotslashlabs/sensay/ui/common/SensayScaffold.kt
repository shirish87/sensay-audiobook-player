package com.dotslashlabs.sensay.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.dotslashlabs.sensay.ui.theme.SpacerSystemBarsBottomPadding

@Composable
fun SensayScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    contentVisible: Boolean,
    contentInitialAlpha: Float = 0.5F,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = 0.dp,
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
        ) {

            AnimatedReveal(visible = contentVisible, initialAlpha = contentInitialAlpha) {
                val insets = WindowInsets.systemGestures.asPaddingValues()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = insets.calculateStartPadding(layoutDirection)),
                ) {
                    content()
                }
            }
        }
    }
}