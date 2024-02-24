package com.dotslashlabs.sensay.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal object ButtonWithIconTokens {
    val StateLayerShape = ShapeKeyTokens.CornerFull
    val StateLayerSize = 40.0.dp
}

internal enum class ShapeKeyTokens {
    CornerExtraLarge,
    CornerExtraLargeTop,
    CornerExtraSmall,
    CornerExtraSmallTop,
    CornerFull,
    CornerLarge,
    CornerLargeEnd,
    CornerLargeTop,
    CornerMedium,
    CornerNone,
    CornerSmall,
}

/**
 * Helper function for component shape tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.shapes.fromToken(FabPrimarySmallTokens.ContainerShape)``
 */
internal fun Shapes.fromToken(value: ShapeKeyTokens): Shape {
    return when (value) {
        ShapeKeyTokens.CornerExtraLarge -> extraLarge
        ShapeKeyTokens.CornerExtraLargeTop -> extraLarge.copy(bottomStart = CornerSize(0.0.dp), bottomEnd = CornerSize(0.0.dp))
        ShapeKeyTokens.CornerExtraSmall -> extraSmall
        ShapeKeyTokens.CornerExtraSmallTop -> extraSmall.copy(bottomStart = CornerSize(0.0.dp), bottomEnd = CornerSize(0.0.dp))
        ShapeKeyTokens.CornerFull -> CircleShape
        ShapeKeyTokens.CornerLarge -> large
        ShapeKeyTokens.CornerLargeEnd -> large.copy(topStart = CornerSize(0.0.dp), bottomStart = CornerSize(0.0.dp))
        ShapeKeyTokens.CornerLargeTop -> large.copy(bottomStart = CornerSize(0.0.dp), bottomEnd = CornerSize(0.0.dp))
        ShapeKeyTokens.CornerMedium -> medium
        ShapeKeyTokens.CornerNone -> RectangleShape
        ShapeKeyTokens.CornerSmall -> small
    }
}

/** Converts a shape token key to the local shape provided by the theme */
@Composable
@ReadOnlyComposable
internal fun ShapeKeyTokens.toShape(): Shape {
    return MaterialTheme.shapes.fromToken(this)
}

@Composable
fun ButtonWithIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(ButtonWithIconTokens.StateLayerSize)
            .clip(ButtonWithIconTokens.StateLayerShape.toShape())
            .then(modifier)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = ButtonWithIconTokens.StateLayerSize / 2,
                ),
            ),
        contentAlignment = Alignment.Center
    ) {

        val contentColor = if (enabled)
            LocalContentColor.current
        else LocalContentColor.current.copy(alpha = 0.38f)

        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
