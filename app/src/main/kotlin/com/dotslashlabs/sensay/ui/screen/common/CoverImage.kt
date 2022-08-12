package com.dotslashlabs.sensay.ui.screen.common

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.dotslashlabs.sensay.R
import logcat.asLog
import logcat.logcat

@Composable
fun CoverImage(
    coverUri: Uri?,
    modifier: Modifier = Modifier,
    drawableResId: Int = R.drawable.empty,
) {
    AsyncImage(
        model = coverUri ?: drawableResId,
        placeholder = painterResource(drawableResId),
        fallback = painterResource(drawableResId),
        onError = { error ->
            logcat("AsyncImage") { "Error loading cover image $coverUri: ${error.result.throwable.asLog()}" }
        },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
