package com.dotslashlabs.sensay.ui.screen.book.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.theme.DynamicThemePrimaryColorsFromImage
import com.dotslashlabs.sensay.ui.theme.MinContrastOfPrimaryVsSurface
import com.dotslashlabs.sensay.ui.theme.contrastAgainst
import com.dotslashlabs.sensay.ui.theme.rememberDominantColorState
import com.dotslashlabs.sensay.util.verticalGradientScrim
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.CoroutineScope

object PlayerScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = PlayerContent(
        activityBridge,
        backStackEntry,
        onBackPress = { navHostController.popBackStack() })
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    @Suppress("UNUSED_PARAMETER") activityBridge: ActivityBridge,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
    @Suppress("UNUSED_PARAMETER") onBackPress: () -> Unit,
) {
    val argsBundle = backStackEntry.arguments ?: return

    val viewModel: PlayerViewModel = mavericksViewModel(argsFactory = { argsBundle })
    val state by viewModel.collectAsState()

    PlayerDynamicTheme(state) {
        SensayFrame(
            modifier = Modifier
                .verticalGradientScrim(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                    startYPercentage = 1f,
                    endYPercentage = 0f,
                ),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        icon = { Icon(Icons.Filled.PlayArrow, "Play") },
                        text = { Text("Play") },
                        onClick = {}
                    )
                },
                topBar = {
                    PlayerAppBar(onBackPress = onBackPress)
                },
                content = { contentPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(36.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            PlayerImage(
                                coverUri = state.coverUri,
                                modifier = Modifier.clickable { onBackPress() },
                            )

                            state.bookProgressWithChapters.invoke()?.let {
                                PlayerButtons(activityBridge, it)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerImage(
    coverUri: Uri?,
    modifier: Modifier = Modifier,
    drawableResId: Int = R.drawable.empty,
) {
    CoverImage(
        coverUri,
        drawableResId = drawableResId,
        modifier = modifier
            .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium),
    )
}

@Composable
private fun PlayerButtons(
    activityBridge: ActivityBridge,
    bookWithChapters: BookProgressWithBookAndChapters,
    modifier: Modifier = Modifier,
    playerButtonSize: Dp = 96.dp,
    sideButtonSize: Dp = 64.dp,
) {
    val context: Context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = activityBridge.mediaController()?.isPlaying == true,
            onClick = {
                activityBridge.mediaController()?.seekBack()
            },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            onClick = {
                activityBridge.mediaController()?.apply {
                    if (currentMediaItem?.mediaId != bookWithChapters.book.bookId.toString()) {
                        setMediaItem(
                            MediaItem.Builder()
                                .setUri(bookWithChapters.book.uri)
                                .setMediaId(bookWithChapters.book.bookId.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(bookWithChapters.book.title)
                                        .setArtist(bookWithChapters.book.author)
                                        .setIsPlayable(true)
                                        .setArtworkUri(bookWithChapters.book.coverUri)
                                        .build()
                                )
                                .setRequestMetadata(
                                    MediaItem.RequestMetadata.Builder()
                                        .setMediaUri(bookWithChapters.book.uri)
                                        .build()
                                )
                                .build()
                        )

                        prepare()
                    }
                }

                activityBridge.mediaController()?.apply {
                    if (isPlaying) {
                        pause()
                        Toast.makeText(context, "Pause", Toast.LENGTH_SHORT).show()
                    } else {
                        playWhenReady = true
                        play()
                        Toast.makeText(context, "Play", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize)
        ) {
            Icon(
                imageVector = if (activityBridge.mediaController()?.isPlaying == true) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = activityBridge.mediaController()?.isPlaying == true,
            onClick = {
                activityBridge.mediaController()?.seekForward()
            },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = null,
            )
        }
    }
}

/**
 * Theme that updates the colors dynamically depending on the podcast image URL
 */
@Composable
private fun PlayerDynamicTheme(
    state: PlayerViewState,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dominantColorState = rememberDominantColorState(
        defaultColor = MaterialTheme.colorScheme.surface
    ) { color ->
        // We want a color which has sufficient contrast against the surface color
        color.contrastAgainst(surfaceColor) >= MinContrastOfPrimaryVsSurface
    }

    DynamicThemePrimaryColorsFromImage(dominantColorState) {
        // Update the dominantColorState with colors coming from the podcast image URL
        LaunchedEffect(state.coverUri) {
            state.coverUri?.let {
                dominantColorState.updateColorsFromImageUrl(it)
            } ?: dominantColorState.reset()
        }

        content()
    }
}
