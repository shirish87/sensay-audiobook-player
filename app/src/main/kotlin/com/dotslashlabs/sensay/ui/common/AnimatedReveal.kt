package com.dotslashlabs.sensay.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable

@Composable
fun AnimatedReveal(
    visible: Boolean,
    initialAlpha: Float = 0.2F,
    content: @Composable() AnimatedVisibilityScope.() -> Unit,
) {

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(initialAlpha = initialAlpha),
        exit = fadeOut(),
        content = content,
    )
}