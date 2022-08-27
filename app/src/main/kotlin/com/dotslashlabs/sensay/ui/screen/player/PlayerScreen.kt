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
import androidx.compose.material.icons.outlined.Undo
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
import com.dotslashlabs.sensay.common.PlaybackConnectionState
import com.dotslashlabs.sensay.ui.SensayAppState
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.BookProgressIndicator
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.theme.DynamicThemePrimaryColorsFromImage
import com.dotslashlabs.sensay.ui.theme.MinContrastOfPrimaryVsSurface
import com.dotslashlabs.sensay.ui.theme.contrastAgainst
import com.dotslashlabs.sensay.ui.theme.rememberDominantColorState
import com.dotslashlabs.sensay.util.PlayerState
import com.dotslashlabs.sensay.util.verticalGradientScrim
import data.entity.Book
import data.entity.BookProgress
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

    val viewModel: PlayerViewModel =
        mavericksViewModel(argsFactory = { PlayerViewArgs(argsBundle) })
    val state by viewModel.collectAsState()

    DisposableEffect(viewModel) {
        viewModel.subscribe()

        onDispose {
            viewModel.unsubscribe()
        }
    }

    PlayerContentView(viewModel, state, useLandscapeLayout, onBackPress)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContentView(
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
                            playerActions,
                            state,
                            modifier = Modifier.padding(contentPadding),
                            onBackPress = onBackPress,
                        )
                    } else {
                        PlayerContentViewNormal(
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
            SelectChapter(playerActions, state)
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerProgress(
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )
        }

        PlayerButtons(
            playerActions,
            state,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun PlayerContentViewLandscape(
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

            SelectChapter(playerActions, state)

            PlayerProgress(
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )

            PlayerButtons(
                playerActions,
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.bookProgress?.let { bookProgress ->
            BookProgressIndicator(
                book = book,
                bookProgress = bookProgress,
                modifier = Modifier.sizeIn(maxWidth = 500.dp),
            )
        }

        Text(
            text = book.author ?: "",
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.titleSmall,
        )

        Text(
            text = book.title,
            textAlign = TextAlign.Center,
            maxLines = 3,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun PlayerProgress(
    playerActions: PlayerActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
) {
    if (!state.isConnected) return

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
                enabled = !state.isSliderDisabled,
                modifier = Modifier.semantics { contentDescription = "Localized Description" },
                value = state.sliderPosition,
                valueRange = 0F..0.99F,
                onValueChange = {
                    playerActions.seekTo(it, duration)
                },
            )
        } else if (state.isLoading || state.isMediaIdPreparing) {
            Row {
                LinearProgressIndicator()
            }
        }
    }
}

@Composable
private fun PlayerButtons(
    playerActions: PlayerActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
    playerButtonSize: Dp = 72.dp,
    sideButtonSize: Dp = 40.dp,
) {

    if (!state.isConnected) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = state.isSelectedMediaIdCurrent && !state.isMediaIdPreparing,
            onClick = { playerActions.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = !state.isMediaIdPreparing,
            onClick = {
                playerActions.apply {
                    if (state.isSelectedMediaIdPlaying) {
                        pause()
                    } else {
                        play()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize),
        ) {
            Icon(
                imageVector = if (state.isSelectedMediaIdPlaying) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = state.isSelectedMediaIdCurrent && !state.isMediaIdPreparing,
            onClick = { playerActions.seekForward() },
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
    playerActions: PlayerActions,
    state: PlayerViewState,
) {

    val chapters = state.chapters
    val chapterMediaIds = state.mediaIds

    // If there are no chapters, its an exception since all books must have at least one
    // We skip displaying chapter selections for books with just 1 chapter
    // since it may be a book chapter that we have "synthetically" added
    if (chapters.size <= 1 || chapters.size != chapterMediaIds.size) return

    val (selectedMediaId, selectedChapter) = state.selectedChapter ?: return

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

            TextField(
                readOnly = true,
                enabled = false,
                value = selectedChapter.title,
                onValueChange = {},
                label = { Text(text = "Chapter") },
                leadingIcon = {
                    if (state.enableResetSelectedMediaId) {
                        IconButton(onClick = {
                            playerActions.resetSelectedMediaId()
                            expanded = false
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Undo,
                                contentDescription = null,
                            )
                        }
                    }
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.disabledTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                val lastIdx = chapters.lastIndex

                chapters.mapIndexed { idx, chapter ->
                    val mediaId = chapterMediaIds[idx]

                    DropdownMenuItem(
                        text = { Text(chapter.title) },
                        leadingIcon = {
                            if (mediaId == selectedMediaId) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            expanded = false

                            if (mediaId != selectedMediaId) {
                                playerActions.setSelectedMediaId(mediaId)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuDefaults.disabledTextFieldColors(): TextFieldColors {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35F)

    return textFieldColors(
        containerColor = containerColor,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}


@Preview
@Composable
fun PlayerContentPreview() {
    val bookId = 2L

    val chapters = listOf(
        Chapter.empty().copy(
            chapterId = 1,
            title = "Chapter 1 Title",
        ),
        Chapter.empty().copy(
            chapterId = 2,
            title = "Chapter 2 Title",
        ),
    )

    val mediaIds = chapters.map { PlayerViewState.getMediaId(bookId, it.chapterId) }

    val state = PlayerViewState(
        bookId = bookId,
        book = Book.empty().copy(
            bookId = bookId,
            title = "Book Title",
            author = "Author",
        ),
        bookProgress = BookProgress.empty(),
        chapters = chapters,
        mediaIds = mediaIds,
        mediaId = mediaIds.first(),
        playbackConnectionState = Success(
            PlaybackConnectionState(
                isConnected = true,
                playerState = PlayerState(
                    isPlaying = false,
                ),
            )
        )
    )

    val playerActions = object : PlayerActions {
        override fun subscribe() {
            TODO("Not yet implemented")
        }

        override fun unsubscribe() {
            TODO("Not yet implemented")
        }

        override fun seekBack(): Unit? {
            TODO("Not yet implemented")
        }

        override fun seekForward(): Unit? {
            TODO("Not yet implemented")
        }

        override fun seekTo(fraction: Float, ofDurationMs: Long): Unit? {
            TODO("Not yet implemented")
        }

        override fun pause(): Unit? {
            TODO("Not yet implemented")
        }

        override fun play(): Unit? {
            TODO("Not yet implemented")
        }

        override fun setSelectedMediaId(mediaId: String) {
            TODO("Not yet implemented")
        }

        override fun resetSelectedMediaId() {
            TODO("Not yet implemented")
        }
    }

    PlayerContentView(
        playerActions,
        state,
        useLandscapeLayout = false,
        onBackPress = {},
    )
}
