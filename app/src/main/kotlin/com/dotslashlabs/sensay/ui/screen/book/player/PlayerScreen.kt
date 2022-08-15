package com.dotslashlabs.sensay.ui.screen.book.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.service.PlaybackConnectionState
import com.dotslashlabs.sensay.ui.PlaybackActions
import com.dotslashlabs.sensay.ui.PlaybackViewModel
import com.dotslashlabs.sensay.ui.SensayAppState
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.theme.DynamicThemePrimaryColorsFromImage
import com.dotslashlabs.sensay.ui.theme.MinContrastOfPrimaryVsSurface
import com.dotslashlabs.sensay.ui.theme.contrastAgainst
import com.dotslashlabs.sensay.ui.theme.rememberDominantColorState
import com.dotslashlabs.sensay.util.verticalGradientScrim
import data.entity.Book
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.Chapter

object PlayerScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = backStackEntry.arguments?.let { args ->
        PlayerContent(args, onBackPress = { navHostController.popBackStack() })
    } ?: Unit
}

@Composable
fun PlayerContent(
    argsBundle: Bundle,
    onBackPress: () -> Unit,
) {

    val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
    val useLandscapeLayout by appViewModel.collectAsState(SensayAppState::useLandscapeLayout)

    val playbackViewModel: PlaybackViewModel = mavericksActivityViewModel()

    val viewModel: PlayerViewModel = mavericksViewModel(argsFactory = { argsBundle })
    val state by viewModel.collectAsState()

    PlayerContentView(playbackViewModel, state, useLandscapeLayout, onBackPress)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContentView(
    playbackActions: PlaybackActions,
    state: PlayerViewState,
    useLandscapeLayout: Boolean,
    onBackPress: () -> Unit,
) {
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
                topBar = {
                    PlayerAppBar(onBackPress = onBackPress)
                },
                content = { contentPadding ->
                    if (useLandscapeLayout) {
                        PlayerContentViewLandscape(
                            playbackActions,
                            state,
                            modifier = Modifier.padding(contentPadding),
                            onBackPress = onBackPress,
                        )
                    } else {
                        PlayerContentViewNormal(
                            playbackActions,
                            state,
                            modifier = Modifier.padding(contentPadding),
                            onBackPress = onBackPress,
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun PlayerContentViewNormal(
    playbackActions: PlaybackActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerImage(
                coverUri = state.coverUri,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 20.dp)
                    .clickable { onBackPress() },
            )
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerInfo(state = state)
        }

        PlayerButtons(
            playbackActions,
            state,
            modifier = Modifier.padding(vertical = 40.dp, horizontal = 20.dp),
        )
    }
}

@Composable
fun PlayerContentViewLandscape(
    playbackActions: PlaybackActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    onBackPress: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(
            modifier = Modifier.weight(0.3F),
        ) {
            PlayerImage(
                coverUri = state.coverUri,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .clickable { onBackPress() },
            )
        }

        Column(
            modifier = Modifier.weight(0.7F),
        ) {
            PlayerInfo(state = state)

            PlayerButtons(
                playbackActions,
                state,
                modifier = Modifier.padding(20.dp),
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
private fun PlayerInfo(
    state: PlayerViewState,
    modifier: Modifier = Modifier,
) {

    Column(modifier = modifier) {
        Text(
            text = state.book?.title ?: "",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = state.book?.author ?: "",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            text = if (state.isCurrentBook) {
                "${state.currentPosition} / ${state.duration}"
            } else {
                "${state.bookProgress?.currentPosition ?: ""} / ${state.bookProgress?.duration ?: ""}"
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PlayerButtons(
    playbackActions: PlaybackActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    playerButtonSize: Dp = 96.dp,
    sideButtonSize: Dp = 64.dp,
) {

    val bookProgressWithChapters = state.bookProgress ?: return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentBook,
            onClick = { playbackActions.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentBook,
            onClick = {
                if (state.isCurrentBook) {
                    // current book
                    playbackActions.apply {
                        if (state.isPlaying) {
                            pause()
                        } else {
                            // play
                            playWhenReady = true
                            play()
                        }
                    }
                } else {
                    // other book, only play action is possible
                    playbackActions.apply {
                        prepareMediaItems(bookProgressWithChapters)
                        playWhenReady = true
                        play()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize),
        ) {
            Icon(
                imageVector = if (state.isCurrentBookPlaying) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentBook,
            onClick = { playbackActions.seekForward() },
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

@Preview
@Composable
fun PlayerContentPreview() {
    val bookId = 2L

    val state = PlayerViewState(
        bookId = bookId,
        bookProgressWithChapters = Success(
            BookProgressWithBookAndChapters(
                book = Book.empty().copy(
                    bookId = bookId,
                ),
                bookProgress = BookProgress.empty(),
                chapter = Chapter.empty(),
                chapters = listOf(Chapter.empty()),
            )
        ),
        playbackConnectionState = Success(
            PlaybackConnectionState(
                isConnected = true,
                isPlaying = false,
            )
        )
    )

    val playbackActions = object : PlaybackActions {
        override var playWhenReady: Boolean = false

        override fun prepareMediaItems(bookProgressWithBookAndChapters: BookProgressWithBookAndChapters) {
            TODO("Not yet implemented")
        }

        override fun seekBack(): Unit? {
            TODO("Not yet implemented")
        }

        override fun seekForward(): Unit? {
            TODO("Not yet implemented")
        }

        override fun pause(): Unit? {
            TODO("Not yet implemented")
        }

        override fun play(): Unit? {
            TODO("Not yet implemented")
        }
    }

    PlayerContentView(
        playbackActions,
        state,
        useLandscapeLayout = false,
        onBackPress = {},
    )
}
