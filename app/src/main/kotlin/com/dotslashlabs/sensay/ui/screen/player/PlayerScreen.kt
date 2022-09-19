package com.dotslashlabs.sensay.ui.screen.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import data.BookCategory
import data.entity.Book
import data.entity.BookProgress
import data.entity.Bookmark
import data.entity.Chapter
import data.util.ContentDuration
import kotlin.time.Duration.Companion.hours

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

@OptIn(ExperimentalMaterialApi::class)
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
    val context = LocalContext.current

    DisposableEffect(viewModel, context) {
        viewModel.attachPlayer(context)

        onDispose {
            viewModel.detachPlayer()
        }
    }

    PlayerBottomSheet(viewModel, state) { bottomSheetState ->
        PlayerContentView(viewModel, state, bottomSheetState, useLandscapeLayout, onBackPress)
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PlayerContentView(
    playerActions: PlayerActions,
    state: PlayerViewState,
    bottomSheetState: ModalBottomSheetState,
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
                    PlayerAppBar(playerActions, state, bottomSheetState, onBackPress = onBackPress)
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(modifier),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerImage(
                coverUri = state.coverUri,
                onClick = onBackPress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            PlayerInfo(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(
            modifier = Modifier.weight(0.3F),
        ) {
            PlayerImage(
                coverUri = state.coverUri,
                onClick = onBackPress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(0.7F),
        ) {
            PlayerInfo(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )

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
    onClick: () -> Unit,
    drawableResId: Int = R.drawable.empty,
) {
    CoverImage(
        coverUri,
        drawableResId = drawableResId,
        modifier = modifier
            .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun PlayerInfo(
    state: PlayerViewState,
    modifier: Modifier = Modifier,
) {

    val book = state.media ?: return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        BookProgressIndicator(
            bookProgressMs = state.media.bookProgress.ms,
            bookDurationMs = state.media.bookDuration.ms,
            modifier = Modifier.sizeIn(maxWidth = 500.dp),
        )

        Text(
            text = "${book.author}",
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.titleSmall,
        )

        Text(
            text = book.bookTitle,
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
//    if (!state.isConnected) return

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
                enabled = !state.isLoading,
                modifier = Modifier.semantics { contentDescription = "Localized Description" },
                value = state.sliderPosition,
                valueRange = 0F..0.99F,
                onValueChange = {
                    playerActions.seekTo(it, duration)
                },
            )
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

//    if (!state.isConnected) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {

        val buttonsModifier = Modifier.size(sideButtonSize)

        OutlinedIconButton(
            enabled = state.isActiveMedia &&
                !state.isLoading &&
                state.hasPreviousChapter,
            onClick = { playerActions.previousChapter() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = state.isActiveMedia && !state.isLoading,
            onClick = { playerActions.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = !state.isLoading,
            onClick = {
                playerActions.apply {
                    if (state.isPlayingMedia) {
                        pause()
                    } else {
                        play()
                    }
                }
            },
            modifier = Modifier.size(playerButtonSize),
        ) {
            Icon(
                imageVector = if (state.isPlayingMedia) {
                    Icons.Filled.PauseCircleFilled
                } else {
                    Icons.Filled.PlayCircleFilled
                },
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }

        OutlinedIconButton(
            enabled = state.isActiveMedia && !state.isLoading,
            onClick = { playerActions.seekForward() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = state.isActiveMedia &&
                !state.isLoading &&
                state.hasNextChapter,
            onClick = { playerActions.nextChapter() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
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

    val chapters = state.mediaList
    val chapterMediaIds = state.mediaIds

    // If there are no chapters, its an exception since all books must have at least one
    // We skip displaying chapter selections for books with just 1 chapter
    // since it may be a book chapter that we have "synthetically" added
    if (chapters.size <= 1 || chapters.size != chapterMediaIds.size) return

    val selectedMedia = state.media ?: return

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
                value = selectedMedia.chapterTitle,
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
                        text = { Text(chapter.chapterTitle) },
                        leadingIcon = {
                            if (mediaId == selectedMedia.mediaId) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            expanded = false

                            if (mediaId != selectedMedia.mediaId) {
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

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
fun PlayerContentPreview() {
    val bookId = 2L

    val book = Book.empty().copy(
        bookId = bookId,
        title = "Book Title",
        author = "Author",
        duration = ContentDuration(5.hours),
    )

    val chapters = listOf(
        Chapter.empty().copy(
            chapterId = 1,
            title = "Chapter 1 Title",
            duration = ContentDuration(1.hours),
        ),
        Chapter.empty().copy(
            chapterId = 2,
            title = "Chapter 2 Title",
            duration = ContentDuration(1.hours),
        ),
    )

    val bookProgress = BookProgress.empty().copy(
        bookCategory = BookCategory.CURRENT,
        currentChapter = 1,
        totalChapters = 2,
        chapterProgress = ContentDuration(1.hours),
        bookProgress = ContentDuration(1.hours),
    )

    val mediaList = Media.fromBookAndChapters(bookProgress, book, chapters)
    val mediaIds = chapters.map { PlayerViewState.getMediaId(bookId, it.chapterId) }

    val state = PlayerViewState(
        bookId = bookId,
        media = mediaList.first(),
        mediaList = mediaList,
        mediaIds = mediaIds,
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
        override fun attachPlayer(context: Context) {
            TODO("Not yet implemented")
        }

        override fun detachPlayer() {
            TODO("Not yet implemented")
        }

        override fun previousChapter(): Unit? {
            TODO("Not yet implemented")
        }

        override fun nextChapter(): Unit? {
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

        override fun seekToPosition(mediaId: String, positionMs: Long, durationMs: Long): Unit? {
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

        override fun createBookmark() {
            TODO("Not yet implemented")
        }

        override fun deleteBookmark(bookmark: Bookmark) {
            TODO("Not yet implemented")
        }
    }

    PlayerBottomSheet(playerActions, state) { bottomSheetState ->
        PlayerContentView(
            playerActions,
            state,
            bottomSheetState,
            useLandscapeLayout = false,
            onBackPress = {},
        )
    }
}
