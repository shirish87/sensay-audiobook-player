package com.dotslashlabs.sensay.ui.screen.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    val viewModel: PlayerViewModel =
        mavericksViewModel(argsFactory = { PlayerViewArgs(argsBundle) })
    val state by viewModel.collectAsState()

    PlayerContentView(playbackViewModel, viewModel, state, useLandscapeLayout, onBackPress)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContentView(
    playbackActions: PlaybackActions,
    playerActions: PlayerActions,
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
                            playerActions,
                            state,
                            modifier = Modifier.padding(contentPadding),
                            onBackPress = onBackPress,
                        )
                    } else {
                        PlayerContentViewNormal(
                            playbackActions,
                            playerActions,
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
    playerActions: PlayerActions,
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

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            SelectChapter(playbackActions, playerActions, state)
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerProgress(
                playbackActions = playbackActions,
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )
        }

        PlayerButtons(
            playbackActions,
            state,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun PlayerContentViewLandscape(
    playbackActions: PlaybackActions,
    playerActions: PlayerActions,
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

            SelectChapter(playbackActions, playerActions, state)

            PlayerProgress(
                playbackActions = playbackActions,
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )

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

    val book = state.book ?: return

    Column(modifier = modifier) {
        Text(
            text = book.author ?: "",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            text = book.title,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun PlayerProgress(
    playbackActions: PlaybackActions,
    playerActions: PlayerActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
) {
    val (position, duration) = state.progressPair

    Column(modifier = modifier) {
        if (duration != null) {
            Row {
                Column(
                    modifier = Modifier.weight(0.5F),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = state.formatTime(position),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(
                    modifier = Modifier.weight(0.5F),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = state.formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Slider(
                modifier = Modifier.semantics { contentDescription = "Localized Description" },
                value = state.sliderPosition,
                valueRange = 0f..99f,
                onValueChange = {
                    playerActions.setSliderPosition(it)
                    playbackActions.seekTo((it * duration).toLong())
                },
            )
        } else if (state.isCurrentMediaId) {
            Row {
                LinearProgressIndicator()
            }
        }
    }
}

@Composable
private fun PlayerButtons(
    playbackActions: PlaybackActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    playerButtonSize: Dp = 72.dp,
    sideButtonSize: Dp = 40.dp,
) {

    if (!state.isConnected) return
    val bookProgressWithChapters = state.bookProgress ?: return
    val selectedChapter = state.selectedChapter ?: return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentMediaId,
            onClick = { playbackActions.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentMediaId,
            onClick = {
                if (state.isCurrentMediaId) {
                    playbackActions.apply {
                        if (state.isPlaying) {
                            pause()
                        } else {
                            play()
                        }
                    }
                } else {
                    playbackActions.apply {
                        prepareMediaItems(bookProgressWithChapters, selectedChapter.chapterId)
                        play()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize),
        ) {
            Icon(
                imageVector = if (state.isPlayingCurrentMediaId) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = !state.isPreparingCurrentMediaId,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectChapter(
    playbackActions: PlaybackActions,
    playerActions: PlayerActions,
    state: PlayerViewState,
) {
    val selectedChapter = state.selectedChapter ?: return

    // If there are no chapters, its an exception since all books must have at least one
    // We skip displaying chapter selections for books with just 1 chapter
    // since it may be a book chapter that we have "synthetically" added
    if (state.chapters.size <= 1) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
            .padding(top = 10.dp),
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            val containerColor = ExposedDropdownMenuDefaults.textFieldColors()
                .containerColor(enabled = true).value.copy(alpha = 0.35F)

            TextField(
                readOnly = true,
                value = selectedChapter.title,
                onValueChange = {},
                label = { Text(text = "Chapter") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults
                    .textFieldColors(containerColor = containerColor),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                val lastIdx = state.chapters.lastIndex

                state.chapters.mapIndexed { idx, c ->
                    DropdownMenuItem(
                        text = { Text(c.title) },
                        leadingIcon = {
                            if (c.chapterId == selectedChapter.chapterId) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            expanded = false

                            if (c.chapterId != selectedChapter.chapterId) {
                                playerActions.setSelectedChapterId(c.chapterId)

                                if (state.isPlaying) {
                                    playbackActions.pause()
                                }
                            }
                        },
                    )

                    if (idx != lastIdx) {
                        Divider()
                    }
                }
            }
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
    val chapterId = 1L

    val state = PlayerViewState(
        bookId = bookId,
        bookProgressWithChapters = Success(
            BookProgressWithBookAndChapters(
                book = Book.empty().copy(
                    bookId = bookId,
                    title = "Book Title",
                    author = "Author",
                ),
                bookProgress = BookProgress.empty(),
                chapter = Chapter.empty().copy(
                    chapterId = chapterId,
                    title = "Chapter Title",
                ),
                chapters = listOf(
                    Chapter.empty().copy(
                        chapterId = chapterId,
                        title = "Chapter 1 Title",
                    ),
                    Chapter.empty().copy(
                        chapterId = 2,
                        title = "Chapter 2 Title",
                    ),
                ),
            )
        ),
        playbackConnectionState = Success(
            PlaybackConnectionState(
                isConnected = true,
            )
        )
    )

    val playbackActions = object : PlaybackActions {
        override var playWhenReady: Boolean = false

        override fun prepareMediaItems(
            bookProgressWithBookAndChapters: BookProgressWithBookAndChapters,
            selectedChapterId: Long?,
        ) {
            TODO("Not yet implemented")
        }

        override fun seekBack(): Unit? {
            TODO("Not yet implemented")
        }

        override fun seekForward(): Unit? {
            TODO("Not yet implemented")
        }

        override fun seekTo(positionMs: Long): Unit? {
            TODO("Not yet implemented")
        }

        override fun pause(): Unit? {
            TODO("Not yet implemented")
        }

        override fun play(): Unit? {
            TODO("Not yet implemented")
        }

        override fun setChapter(chapterId: Long) {
            TODO("Not yet implemented")
        }
    }

    val playerActions = object : PlayerActions {
        override fun setSelectedChapterId(chapterId: Long) {
            TODO("Not yet implemented")
        }

        override fun setSliderPosition(position: Float) {
            TODO("Not yet implemented")
        }
    }

    PlayerContentView(
        playbackActions,
        playerActions,
        state,
        useLandscapeLayout = false,
        onBackPress = {},
    )
}
