package com.dotslashlabs.sensay.ui.screen.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.ui.*
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.BookProgressIndicator
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookAuthorAndSeries
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookTitleAndChapter
import com.dotslashlabs.sensay.ui.theme.DynamicThemePrimaryColorsFromImage
import com.dotslashlabs.sensay.ui.theme.MinContrastOfPrimaryVsSurface
import com.dotslashlabs.sensay.ui.theme.contrastAgainst
import com.dotslashlabs.sensay.ui.theme.rememberDominantColorState
import com.dotslashlabs.sensay.util.verticalGradientScrim
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import logcat.logcat

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

    val playerAppViewModel: PlayerAppViewModel = mavericksActivityViewModel()
    val playerAppViewState: PlayerAppViewState by playerAppViewModel.collectAsState()

    val viewModel: PlayerViewModel =
        mavericksViewModel(argsFactory = { PlayerViewArgs(argsBundle) })
    val state by viewModel.collectAsState()

    PlayerBottomSheet(playerAppViewModel, viewModel, state) { bottomSheetState ->
        PlayerContentView(
            playerAppViewModel,
            playerAppViewState,
            viewModel,
            state,
            bottomSheetState,
            useLandscapeLayout,
            onBackPress,
        )
    }

    val context = LocalContext.current

    DisposableEffect(viewModel, context) {
        logcat { "PlayerContent.attachPlayer" }
        viewModel.attachPlayer(context)

        onDispose {
            logcat { "PlayerContent.detachPlayer" }
            viewModel.detachPlayer()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PlayerContentView(
    playerAppViewActions: PlayerAppViewActions,
    playerAppViewState: PlayerAppViewState,
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

                    AnimatedVisibility(
                        visible = state.isPlayerLoaded,
                        enter = fadeIn(initialAlpha = 0.4F),
                        exit = fadeOut(),
                    ) {
                        if (useLandscapeLayout) {
                            PlayerContentViewLandscape(
                                playerAppViewActions,
                                playerAppViewState,
                                playerActions,
                                state,
                                modifier = Modifier.padding(contentPadding),
                                onBackPress = onBackPress,
                            )
                        } else {
                            PlayerContentViewNormal(
                                playerAppViewActions,
                                playerAppViewState,
                                playerActions,
                                state,
                                modifier = Modifier.padding(contentPadding),
                                onBackPress = onBackPress,
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PlayerContentViewNormal(
    playerAppViewActions: PlayerAppViewActions,
    playerAppViewState: PlayerAppViewState,
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

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isEqPanelVisible) {
                    PlayerEq(playerAppViewActions, playerActions, state)
                }

                PlayerImage(
                    coverUri = state.coverUri,
                    onClick = onBackPress,
                    modifier = Modifier
                        .fillMaxSize(fraction = if (state.isEqPanelVisible) 0.65F else 1F)
                        .padding(horizontal = 40.dp, vertical = 20.dp),
                )
            }
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
                playerAppViewActions = playerAppViewActions,
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )
        }

        PlayerButtons(
            playerAppViewActions,
            playerAppViewState,
            playerActions,
            state,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun PlayerContentViewLandscape(
    playerAppViewActions: PlayerAppViewActions,
    playerAppViewState: PlayerAppViewState,
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
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.isEqPanelVisible) {
                PlayerEq(playerAppViewActions, playerActions, state)
            }

            PlayerImage(
                coverUri = state.coverUri,
                onClick = onBackPress,
                modifier = Modifier
                    .fillMaxSize(fraction = if (state.isEqPanelVisible) 0.65F else 1F)
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
                playerAppViewActions = playerAppViewActions,
                playerActions = playerActions,
                state = state,
                modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
            )

            PlayerButtons(
                playerAppViewActions,
                playerAppViewState,
                playerActions,
                state,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerEq(
    playerAppViewActions: PlayerAppViewActions,
    playerActions: PlayerActions,
    state: PlayerViewState,
    modifier: Modifier = Modifier,
) {

    val eqState = state.bookConfig() ?: return

    FlowRow(
        modifier = modifier,
        mainAxisSpacing = 10.dp,
    ) {

        InputChip(
            selected = eqState.isReverbEnabled,
            label = { Text("Reverb") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SpatialAudio,
                    contentDescription = "",
                )
            },
            onClick = {
                playerActions.toggleReverb(
                    playerAppViewActions,
                    !eqState.isReverbEnabled,
                )
            },
        )

        InputChip(
            selected = eqState.isVolumeBoostEnabled,
            label = { Text("Volume Boost") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Speaker,
                    contentDescription = "",
                )
            },
            onClick = {
                playerActions.toggleVolumeBoost(
                    playerAppViewActions,
                    !eqState.isVolumeBoostEnabled,
                )
            },
        )

        InputChip(
            selected = eqState.isBassBoostEnabled,
            label = { Text("Bass Boost") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SurroundSound,
                    contentDescription = "",
                )
            },
            onClick = {
                playerActions.toggleBassBoost(
                    playerAppViewActions,
                    !eqState.isBassBoostEnabled,
                )
            },
        )

        InputChip(
            selected = eqState.isSkipSilenceEnabled,
            label = { Text("Skip Silence") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = "",
                )
            },
            onClick = {
                playerActions.toggleSkipSilence(
                    playerAppViewActions,
                    !eqState.isSkipSilenceEnabled,
                )
            },
        )
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {

        BookAuthorAndSeries(
            author = book.author,
            series = book.series,
            bookTitle = book.bookTitle,
            mainAxisAlignment = FlowMainAxisAlignment.Center,
            style = MaterialTheme.typography.titleSmall,
            maxLengthEach = 100,
        )

        BookTitleAndChapter(
            bookTitle = book.bookTitle,
            bookTitleMaxLines = 4,
            bookTitleStyle = MaterialTheme.typography.titleMedium,
            chapterTitle = null,
        )

        BookProgressIndicator(
            bookProgressMs = state.media.bookProgress.ms,
            bookDurationMs = state.media.bookDuration.ms,
            modifier = Modifier
                .sizeIn(maxWidth = 500.dp)
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun PlayerProgress(
    playerAppViewActions: PlayerAppViewActions,
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
                        text = PlayerAppViewState.formatTime(position),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(
                    modifier = Modifier.weight(0.5F),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = PlayerAppViewState.formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Slider(
                enabled = state.isPlayerLoaded,
                modifier = Modifier.semantics { contentDescription = "Localized Description" },
                value = state.sliderPosition,
                valueRange = 0F..0.99F,
                onValueChange = {
                    playerActions.seekTo(playerAppViewActions, it, duration)
                },
            )
        }
    }
}

@Composable
private fun PlayerButtons(
    playerAppViewActions: PlayerAppViewActions,
    playerAppViewState: PlayerAppViewState,
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
                state.isPlayerLoaded &&
                state.hasPreviousChapter,
            onClick = { playerActions.previousChapter(playerAppViewActions, playerAppViewState) },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = state.isActiveMedia && state.isPlayerLoaded,
            onClick = { playerAppViewActions.seekBack() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = state.isPlayerLoaded && !state.isPreparing,
            onClick = {
                if (state.isPlayingMedia) {
                    playerAppViewActions.pause()
                } else {
                    playerActions.play(playerAppViewActions)
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
            enabled = state.isActiveMedia && state.isPlayerLoaded,
            onClick = { playerAppViewActions.seekForward() },
            modifier = buttonsModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = null,
            )
        }

        OutlinedIconButton(
            enabled = state.isActiveMedia &&
                state.isPlayerLoaded &&
                state.hasNextChapter,
            onClick = { playerActions.nextChapter(playerAppViewActions, playerAppViewState) },
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
                modifier = Modifier.menuAnchor(),
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

                val currentChapterIdx = chapterMediaIds.indexOfFirst { mediaId ->
                    (mediaId == selectedMedia.mediaId)
                }

                chapters.mapIndexed { idx, chapter ->
                    val mediaId = chapterMediaIds[idx]

                    DropdownMenuItem(
                        text = { Text(chapter.chapterTitle) },
                        leadingIcon = {
                            if (idx == currentChapterIdx) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            expanded = false

                            if (idx != currentChapterIdx) {
                                playerActions.setSelectedMediaId(mediaId)
                            }
                        },
                    )

                    val lastIdx = chapters.lastIndex
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

//@OptIn(ExperimentalMaterialApi::class)
//@Preview(showBackground = true)
//@Composable
//fun PlayerContentPreview() {
//    val bookId = 2L
//
//    val book = Book.empty().copy(
//        bookId = bookId,
//        title = "Book Title",
//        author = "Author",
//        duration = ContentDuration(5.hours),
//    )
//
//    val chapters = listOf(
//        Chapter.empty().copy(
//            chapterId = 1,
//            title = "Chapter 1 Title",
//            duration = ContentDuration(1.hours),
//        ),
//        Chapter.empty().copy(
//            chapterId = 2,
//            title = "Chapter 2 Title",
//            duration = ContentDuration(1.hours),
//        ),
//    )
//
//    val bookProgress = BookProgress.empty().copy(
//        bookCategory = BookCategory.CURRENT,
//        currentChapter = 1,
//        totalChapters = 2,
//        chapterProgress = ContentDuration(1.hours),
//        bookProgress = ContentDuration(1.hours),
//    )
//
//    val mediaList = Media.fromBookAndChapters(bookProgress, book, chapters)
//    val mediaIds = chapters.map { PlayerViewState.getMediaId(bookId, it.chapterId) }
//
//    val state = PlayerViewState(
//        bookId = bookId,
//        media = mediaList.first(),
//        mediaList = mediaList,
//        mediaIds = mediaIds,
//        playbackConnectionState = Success(
//            PlaybackConnectionState(
//                isConnected = true,
//                playerState = PlayerState(
//                    isPlaying = false,
//                ),
//            )
//        )
//    )
//
//    val playerActions = object : PlayerActions {
//        override fun attachPlayer(context: Context) {
//            TODO("Not yet implemented")
//        }
//
//        override fun detachPlayer() {
//            TODO("Not yet implemented")
//        }
//
//        override fun previousChapter(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun nextChapter(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun seekBack(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun seekForward(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun seekTo(fraction: Float, ofDurationMs: Long): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun seekToPosition(mediaId: String, positionMs: Long, durationMs: Long): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun pause(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun play(): Unit? {
//            TODO("Not yet implemented")
//        }
//
//        override fun setSelectedMediaId(mediaId: String) {
//            TODO("Not yet implemented")
//        }
//
//        override fun resetSelectedMediaId() {
//            TODO("Not yet implemented")
//        }
//
//        override suspend fun createBookmark() {
//            TODO("Not yet implemented")
//        }
//
//        override fun deleteBookmark(bookmark: Bookmark) {
//            TODO("Not yet implemented")
//        }
//
//        override fun toggleEqPanel(isVisible: Boolean) {
//            TODO("Not yet implemented")
//        }
//
//        override fun toggleVolumeBoost(isEnabled: Boolean) {
//            TODO("Not yet implemented")
//        }
//
//        override fun toggleBassBoost(isEnabled: Boolean) {
//            TODO("Not yet implemented")
//        }
//
//        override fun toggleReverb(isEnabled: Boolean) {
//            TODO("Not yet implemented")
//        }
//
//        override fun toggleSkipSilence(isEnabled: Boolean) {
//            TODO("Not yet implemented")
//        }
//    }
//
//    PlayerBottomSheet(playerActions, state) { bottomSheetState ->
//        PlayerContentView(
//            playerActions,
//            state,
//            bottomSheetState,
//            useLandscapeLayout = false,
//            onBackPress = {},
//        )
//    }
//}
