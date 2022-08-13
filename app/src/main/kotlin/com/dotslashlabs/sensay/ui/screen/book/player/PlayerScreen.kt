package com.dotslashlabs.sensay.ui.screen.book.player

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.ui.PlaybackState
import com.dotslashlabs.sensay.ui.PlaybackViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.theme.DynamicThemePrimaryColorsFromImage
import com.dotslashlabs.sensay.ui.theme.MinContrastOfPrimaryVsSurface
import com.dotslashlabs.sensay.ui.theme.contrastAgainst
import com.dotslashlabs.sensay.ui.theme.rememberDominantColorState
import com.dotslashlabs.sensay.util.verticalGradientScrim

object PlayerScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = PlayerContent(
        backStackEntry,
        onBackPress = { navHostController.popBackStack() })
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    backStackEntry: NavBackStackEntry,
    onBackPress: () -> Unit,
) {
    val argsBundle = backStackEntry.arguments ?: return

    val playbackViewModel: PlaybackViewModel = mavericksActivityViewModel()

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
                            val isConnected by playbackViewModel.collectAsState(PlaybackState::isConnected)
                            if (!isConnected) return@Box

                            PlayerImage(
                                coverUri = state.coverUri,
                                modifier = Modifier.clickable { onBackPress() },
                            )

                            PlayerButtons(playbackViewModel, state)
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
    playbackViewModel: PlaybackViewModel,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    playerButtonSize: Dp = 96.dp,
    sideButtonSize: Dp = 64.dp,
) {

    val bookProgressWithChapters = state.bookProgressWithChapters() ?: return
    val playbackState by playbackViewModel.collectAsState()

    val isPreparing = playbackState.isPreparing
    val isCurrentBook = playbackState.isCurrentBook(bookProgressWithChapters.book)
    val isPlaying = playbackState.isPlaying
    val isCurrentBookPlaying = (isCurrentBook && isPlaying)

    Text(
        text = bookProgressWithChapters.book.title,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        style = MaterialTheme.typography.headlineMedium,
    )

    bookProgressWithChapters.book.author?.let { author ->
        Text(
            text = author,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = (!isPreparing && isCurrentBook),
            onClick = { playbackViewModel.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = !isPreparing,
            onClick = {
                if (isCurrentBook) {
                    // current book
                    playbackViewModel.apply {
                        if (isPlaying) {
                            pause()
                        } else {
                            // play
                            playWhenReady = true
                            play()
                        }
                    }
                } else {
                    // other book, only play action is possible
                    playbackViewModel.apply {
                        prepareMediaItems(bookProgressWithChapters)
                        playWhenReady = true
                        play()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize),
        ) {
            Icon(
                imageVector = if (isCurrentBookPlaying) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = (!isPreparing && isCurrentBook),
            onClick = { playbackViewModel.seekForward() },
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
