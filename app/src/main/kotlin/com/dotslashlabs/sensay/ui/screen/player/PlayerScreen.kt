package com.dotslashlabs.sensay.ui.screen.player

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.common.joinWith
import com.dotslashlabs.sensay.ui.common.AnimatedReveal
import com.dotslashlabs.sensay.ui.common.ButtonWithIcon
import com.dotslashlabs.sensay.ui.common.LocalWindowSize
import com.dotslashlabs.sensay.ui.common.SensayScaffold
import com.dotslashlabs.sensay.ui.common.TopProgressAppBar
import com.dotslashlabs.sensay.ui.nav.AppScreen
import com.dotslashlabs.sensay.ui.nav.Destination
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewModel
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewState
import compose.icons.FeatherIcons
import compose.icons.MaterialIcons
import compose.icons.TablerIcons
import compose.icons.feathericons.ChevronLeft
import compose.icons.feathericons.ChevronRight
import compose.icons.materialicons.BookmarkAdd
import compose.icons.materialicons.Bookmarks
import compose.icons.materialicons.GraphicEq
import compose.icons.tablericons.PlayerPauseFilled
import compose.icons.tablericons.PlayerPlayFilled
import compose.icons.tablericons.PlayerTrackNext
import compose.icons.tablericons.PlayerTrackPrev
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat
import media.MediaPlayerState
import media.chapterIndex
import kotlin.time.Duration.Companion.milliseconds


object PlayerScreen : AppScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @UnstableApi
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val args = backStackEntry.arguments ?: Bundle.EMPTY
        val onBackPress: () -> Unit = { navHostController.popBackStack() }

        val nowPlayingViewModel: NowPlayingViewModel = mavericksActivityViewModel()
        val nowPlayingViewState by nowPlayingViewModel.collectAsState()

        val viewModel: PlayerViewModel = mavericksViewModel(backStackEntry, argsFactory = { args })
        val viewState by viewModel.collectAsState()

        logcat { "${viewState.visibleMediaItem?.mediaMetadata?.toBundle()}" }
        logcat { "title=${viewState.visibleMediaItem?.mediaMetadata?.title}" }
        logcat { "artist=${viewState.visibleMediaItem?.mediaMetadata?.artist}" }
        logcat { "albumTitle=${viewState.visibleMediaItem?.mediaMetadata?.albumTitle}" }
        logcat { "albumArtist=${viewState.visibleMediaItem?.mediaMetadata?.albumArtist}" }
        logcat { "composer=${viewState.visibleMediaItem?.mediaMetadata?.composer}" }
        logcat { "conductor=${viewState.visibleMediaItem?.mediaMetadata?.conductor}" }

        var openBottomSheet by rememberSaveable { mutableStateOf(false) }

        SensayScaffold(
            contentVisible = viewState.isReady,
            topBar = {
                TopProgressAppBar(
                    isBusy = !viewState.isReady,
                    title = {},
                    onBackPress = onBackPress,
                    actions = {
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = MaterialIcons.GraphicEq,
                                contentDescription = "",
                            )
                        }
                        IconButton(
                            onClick = {},
                        ) {
                            Icon(
                                imageVector = MaterialIcons.BookmarkAdd,
                                contentDescription = "",
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = MaterialIcons.Bookmarks,
                                contentDescription = "",
                            )
                        }
                    },
                )
            },
        ) {

            if (LocalWindowSize.current.isLandscape) {
                MainLandscape(
                    viewModel,
                    viewState,
                    nowPlayingViewModel,
                    nowPlayingViewState,
                )
            } else {
                MainPortrait(
                    viewModel,
                    viewState,
                    nowPlayingViewModel,
                    nowPlayingViewState,
                )
            }
        }

        PlayerChapterListModalSheet(
            viewState,
            nowPlayingViewState,
            openBottomSheet,
            { openBottomSheet = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        DisposableEffect(
            viewState.bookId,
            nowPlayingViewState.isPlayerAttached,
        ) {

            if (nowPlayingViewState.isPlayerAttached) {
                viewModel.loadContextMedia(nowPlayingViewModel::resolveBook)
            }

            onDispose {}
        }

        DisposableEffect(
            nowPlayingViewState.isPlayerAttached,
            viewState.visiblePlayerState()?.currentMediaId,
            nowPlayingViewState.playerState()?.currentMediaId,
        ) {

            if (nowPlayingViewState.isPlayerAttached) {
                if (nowPlayingViewState.isCurrent(viewState)) {
                    nowPlayingViewModel.setActive(true, 1000L)
                } else {
                    nowPlayingViewModel.setActive(false)
                }
            }

            onDispose {}
        }
    }
}

@Composable
@UnstableApi
fun MainLandscape(
    viewModel: PlayerViewModel,
    viewState: PlayerViewState,
    nowPlayingViewModel: NowPlayingViewModel,
    nowPlayingViewState: NowPlayingViewState,
) {

    val horizontalSpacing = 8.dp

    val mediaItems = viewState.contextPlaylistState()?.mediaItems ?: return
    val listState = rememberLazyListState(viewModel.chapterIndex, 0)
    val currentMediaId = nowPlayingViewState.playerState()?.currentMediaId

    val currentMediaListIndex = remember(mediaItems, currentMediaId) {
        derivedStateOf {
            mediaItems.indexOfFirst { it.mediaId == currentMediaId }
        }
    }

    val isLoading = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val triggerLoading: () -> Unit = {
        scope.launch {
            isLoading.value = true
            withContext(Dispatchers.IO) {
                delay(200)
            }
            isLoading.value = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = spacedBy(horizontalSpacing, Alignment.CenterHorizontally),
    ) {

        Column(
            modifier = Modifier.weight(0.3F),
            verticalArrangement = Arrangement.Top,
        ) {

            PlayerChapterCover(
                isLoading.value,
                mediaItems,
                listState,
                modifier = Modifier
                    .padding(horizontal = horizontalSpacing, vertical = horizontalSpacing),
            )
        }

        Column(
            modifier = Modifier.weight(0.7F),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            PlayerTitleRow(
                viewState,
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = horizontalSpacing, vertical = 0.dp),
            )

            PlayerChapterRow(
                isLoading.value,
                mediaItems,
                listState,
                currentMediaId,
                viewModel.chapterIndex,
                currentMediaListIndex,
                viewState,
                triggerLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalSpacing, vertical = 16.dp),
            )

            PlayerProgress(
                viewState,
                nowPlayingViewState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalSpacing),
                onSeekTo = nowPlayingViewModel::seekTo,
            )

            PlayerControls(
                viewState,
                nowPlayingViewState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalSpacing),
                onSeekBack = nowPlayingViewModel::seekBack,
                onSeekForward = nowPlayingViewModel::seekForward,
                onPrevious = nowPlayingViewModel::previous,
                onNext = nowPlayingViewModel::next,
                withVisibleMediaAsCurrent = nowPlayingViewModel::withVisibleMediaAsCurrent,
            )
        }
    }

    LaunchedEffect(currentMediaListIndex.value) {
        snapshotFlow { currentMediaListIndex.value }
            .distinctUntilChanged()
            .collectLatest { currentMediaListIndex ->
                currentMediaListIndex
                    .takeIf { it != -1 }
                    ?.let { index -> listState.animateScrollToItem(index, 0) }
            }
    }

    LaunchedEffect(listState, viewModel) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (index in mediaItems.indices) {
                    viewModel.setVisibleMediaItem(mediaItems[index])
                }
            }
    }
}

@Composable
@UnstableApi
fun MainPortrait(
    viewModel: PlayerViewModel,
    viewState: PlayerViewState,
    nowPlayingViewModel: NowPlayingViewModel,
    nowPlayingViewState: NowPlayingViewState,
) {

    val verticalSpacing = 16.dp
    val horizontalSpacing = 8.dp

    val mediaItems = viewState.contextPlaylistState()?.mediaItems ?: return
    val listState = rememberLazyListState(viewModel.chapterIndex, 0)
    val currentMediaId = nowPlayingViewState.playerState()?.currentMediaId

    val currentMediaListIndex = remember(mediaItems, currentMediaId) {
        derivedStateOf {
            mediaItems.indexOfFirst { it.mediaId == currentMediaId }
        }
    }

    val isLoading = rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val triggerLoading: () -> Unit = {
        scope.launch {
            isLoading.value = true
            withContext(Dispatchers.IO) {
                delay(200)
            }
            isLoading.value = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = spacedBy(verticalSpacing, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        PlayerTitleRow(
            viewState,
            modifier = Modifier
                .padding(horizontal = horizontalSpacing, vertical = 0.dp),
        )

        PlayerChapterCover(
            isLoading.value,
            mediaItems,
            listState,
            modifier = Modifier
                .padding(horizontal = horizontalSpacing, vertical = horizontalSpacing),
        )

        PlayerChapterRow(
            isLoading.value,
            mediaItems,
            listState,
            currentMediaId,
            viewModel.chapterIndex,
            currentMediaListIndex,
            viewState,
            triggerLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalSpacing, vertical = 8.dp),
        )

        PlayerProgress(
            viewState,
            nowPlayingViewState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalSpacing),
            onSeekTo = nowPlayingViewModel::seekTo,
        )

        PlayerControls(
            viewState,
            nowPlayingViewState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalSpacing),
            onSeekBack = nowPlayingViewModel::seekBack,
            onSeekForward = nowPlayingViewModel::seekForward,
            onPrevious = nowPlayingViewModel::previous,
            onNext = nowPlayingViewModel::next,
            withVisibleMediaAsCurrent = nowPlayingViewModel::withVisibleMediaAsCurrent,
        )
    }

    LaunchedEffect(currentMediaListIndex.value) {
        snapshotFlow { currentMediaListIndex.value }
            .distinctUntilChanged()
            .collectLatest { currentMediaListIndex ->
                currentMediaListIndex
                    .takeIf { it != -1 }
                    ?.let { index -> listState.animateScrollToItem(index, 0) }
            }
    }

    LaunchedEffect(listState, viewModel) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (index in mediaItems.indices) {
                    viewModel.setVisibleMediaItem(mediaItems[index])
                }
            }
    }
}


@UnstableApi
@Composable
fun PlayerTitleRow(
    viewState: PlayerViewState,
    modifier: Modifier = Modifier,
) {

    val labelBackgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85F)

    viewState.visibleMediaItem?.let { mediaItem ->
        Surface(
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(6.dp),
            modifier = modifier,
        ) {
            Text(
                "${mediaItem.mediaMetadata.compilation ?: mediaItem.mediaMetadata.albumTitle}",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(labelBackgroundColor)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            )
        }
    } ?: Text("Title not visible")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@UnstableApi
@Composable
fun PlayerChapterCover(
    isLoading: Boolean,
    mediaItems: List<MediaItem>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {

    val context = LocalContext.current
    val labelBackgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85F)

    LazyRow(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        horizontalArrangement = spacedBy(20.dp),
    ) {

        items(mediaItems, key = { it.mediaId }) { mediaItem ->

            val hasImage = (mediaItem.mediaMetadata.artworkUri != null
                    || mediaItem.mediaMetadata.artworkData != null)

            val boxModifier: Modifier = if (hasImage) {
                Modifier
                    .fillParentMaxWidth()
                    .fillParentMaxHeight(0.5F)
                    .aspectRatio(1F)
                    .then(modifier)
                    .shadow(6.dp, shape = RoundedCornerShape(8.dp), clip = true)
            } else {
                Modifier
                    .fillParentMaxWidth()
                    .fillParentMaxHeight(0.5F)
                    .clip(RoundedCornerShape(8.dp))
            }

            Box(modifier = boxModifier) {

                AnimatedReveal(visible = !isLoading) {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = ImageRequest.Builder(context)
                            .data(
                                mediaItem.mediaMetadata.artworkUri
                                    ?: mediaItem.mediaMetadata.artworkData
                            )
                            .diskCacheKey(mediaItem.mediaId)
                            .build(),
                        contentDescription = mediaItem.mediaMetadata.title?.let { "" + it },
                    )
                }

                BookLabel(
                    listOfNotNull(
                        (mediaItem.mediaMetadata.artist ?: mediaItem.mediaMetadata.albumArtist)?.toString(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(labelBackgroundColor)
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun PlayerChapterRow(
    isLoading: Boolean,
    mediaItems: List<MediaItem>,
    listState: LazyListState,
    currentMediaId: String?,
    currentChapterIndex: Int,
    currentMediaListIndex: State<Int>,
    viewState: PlayerViewState,
    triggerLoading: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val scope = rememberCoroutineScope()
    val labelBackgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85F)

    val scrollToDefault: () -> Unit = {
        (currentMediaListIndex.value.takeIf { it != -1 } ?: currentChapterIndex)
            .let { index ->
                scope.launch {
                    listState.scrollToItem(index, 0)
                }
            }
    }

    viewState.visibleMediaItem?.let { mediaItem ->
        val chapterCount = mediaItem.mediaMetadata.totalTrackCount ?: 0
        val chapterIndex = mediaItem.chapterIndex

        Row(
            modifier = modifier
                .height(80.dp)
                .shadow(6.dp, shape = RoundedCornerShape(6.dp), clip = true)
                .background(labelBackgroundColor),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            IconButton(
                modifier = Modifier
                    .weight(0.17F)
                    .fillMaxHeight(),
                enabled = (chapterIndex in 1 until chapterCount),
                onClick = {
                    scope.launch {
                        val newIndex = chapterIndex - 1

                        mediaItems.getOrNull(newIndex)?.let {
                            triggerLoading()
                            listState.animateScrollToItem(newIndex, 0)
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = FeatherIcons.ChevronLeft,
                    contentDescription = null,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.7F)
                    .background(
                        color = if (mediaItem.mediaId == currentMediaId)
                            MaterialTheme.colorScheme.background
                        else Color.Transparent,
                    ),
            )

            Column(
                modifier = Modifier
                    .weight(0.66F)
                    .fillMaxHeight()
                    .combinedClickable(onClick = {}, onLongClick = scrollToDefault)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {

                AnimatedReveal(visible = !isLoading) {
                    Text(
                        text = mediaItem.mediaMetadata.title.toString(),
                        fontWeight = if (mediaItem.mediaId == currentMediaId)
                            FontWeight.Bold
                        else FontWeight.Normal,
                    )
                }

                AnimatedReveal(visible = !isLoading) {
                    Text(
                        text = listOf(
                            formatTime(mediaItem.clippingConfiguration.startPositionMs),
                            formatTime(mediaItem.clippingConfiguration.endPositionMs),
                        ).joinToString(" - "),
                    )
                }
            }

            IconButton(
                modifier = Modifier
                    .weight(0.17F)
                    .fillMaxHeight(),
                enabled = (chapterIndex in 0 until chapterCount - 1),
                onClick = {
                    scope.launch {
                        val newIndex = chapterIndex + 1

                        mediaItems.getOrNull(newIndex)?.let {
                            triggerLoading()
                            listState.animateScrollToItem(newIndex, 0)
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = FeatherIcons.ChevronRight,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@ExperimentalLayoutApi
fun BookLabel(
    components: List<String?>,
    modifier: Modifier = Modifier,
    maxLengthEach: Int = 40,
    maxLines: Int = 2,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {

    components.filterNotNull()
        .takeIf { it.isNotEmpty() }
        ?.run {

            FlowRow(
                modifier = modifier.fillMaxWidth(0.95F),
                horizontalArrangement = horizontalArrangement,
            ) {
                joinWith(" ${Typography.bullet} ")
                    .forEach { text ->
                        Text(
                            text = text.run {
                                if (length > maxLengthEach)
                                    "${substring(0, maxLengthEach)}${Typography.ellipsis}"
                                else this
                            },
                            style = style,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
        }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer",
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8B5B5),
                Color(0xFF8F8B8B),
                Color(0xFFB8B5B5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerChapterListModalSheet(
    viewState: PlayerViewState,
    nowPlayingViewState: NowPlayingViewState,
    openBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val skipPartiallyExpanded by rememberSaveable { mutableStateOf(false) }
    val edgeToEdgeEnabled by rememberSaveable { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    if (openBottomSheet) {
        val windowInsets = if (edgeToEdgeEnabled)
            WindowInsets(0) else BottomSheetDefaults.windowInsets

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = bottomSheetState,
            windowInsets = windowInsets,
        ) {
            PlayerChapterList(
                viewState,
                nowPlayingViewState,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun PlayerChapterList(
    viewState: PlayerViewState,
    nowPlayingViewState: NowPlayingViewState,
    modifier: Modifier = Modifier,
) {

    val nowPlayingMediaId = nowPlayingViewState.playerState()?.currentMediaId

    when (viewState.contextPlaylistState) {
        is Loading -> {
            Box(modifier) {
                CircularProgressIndicator(modifier.align(Alignment.Center))
            }
        }
        is Fail -> {
            Box(modifier) {
                ListItem(
                    headlineContent = { Text((viewState.contextPlaylistState as Fail<*>).error.message ?: "Error") },
                )
            }
        }
        is Success -> {
            val chapters = viewState.contextPlaylistState()?.mediaItems ?: emptyList()

            LazyColumn(
                state = rememberLazyListState(),
                modifier = modifier,
            ) {

                items(chapters, key = { it.mediaId }) { mediaItem ->
                    ListItem(
                        headlineContent = { Text(mediaItem.mediaMetadata.title.toString()) },
                        supportingContent = {
                            Text(
                                text = listOf(
                                    formatTime(mediaItem.clippingConfiguration.startPositionMs),
                                    formatTime(mediaItem.clippingConfiguration.endPositionMs),
                                ).joinToString(" - ")
                            )
                        },
                        leadingContent = {
                            if (mediaItem.mediaId == nowPlayingMediaId) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Localized description",
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
        else -> {}
    }
}


@Composable
@UnstableApi
fun PlayerControls(
    viewState: PlayerViewState,
    nowPlayingViewState: NowPlayingViewState,
    modifier: Modifier = Modifier,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onPrevious: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNext: () -> Unit,
    withVisibleMediaAsCurrent: (PlayerViewState, (Player) -> Unit) -> Unit,
) {

    val playerState by remember(viewState.visiblePlayerState, nowPlayingViewState.playerState) {
        derivedStateOf {
            if (nowPlayingViewState.isCurrent(viewState))
                nowPlayingViewState.playerState
            else
                viewState.visiblePlayerState
        }
    }

    AnimatedVisibility(visible = playerState !is Uninitialized) {
        Row(
            modifier = modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {

            if (playerState is Loading || playerState is Fail) {
                Box(Modifier.fillMaxSize()) {
                    when (playerState) {
                        is Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        is Fail -> Text(
                            text = "Error: ${(playerState as Fail<MediaPlayerState>).error.message}",
                            modifier = Modifier.align(Alignment.Center),
                        )

                        else -> Unit
                    }
                }

                return@Row
            }

            val mediaPlayerState = playerState()
            val buttonsModifier = Modifier
                .aspectRatio(1F)
                .weight(0.33F)

            ButtonWithIcon(
                enabled = (mediaPlayerState?.flags?.isSeekBackEnabled == true),
                onClick = onSeekBack,
                modifier = buttonsModifier,
            ) {
                Icon(
                    imageVector = TablerIcons.PlayerTrackPrev,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }

            ButtonWithIcon(
                enabled = (mediaPlayerState?.flags?.isPlayPauseEnabled == true),
                onClick = {
                    withVisibleMediaAsCurrent(viewState) { player ->
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    }
                },
                modifier = buttonsModifier,
            ) {
                Icon(
                    imageVector = if (mediaPlayerState?.isPlaying == true)
                        TablerIcons.PlayerPauseFilled
                    else TablerIcons.PlayerPlayFilled,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            }

            ButtonWithIcon(
                enabled = (mediaPlayerState?.flags?.isSeekForwardEnabled == true),
                onClick = onSeekForward,
                modifier = buttonsModifier,
            ) {
                Icon(
                    imageVector = TablerIcons.PlayerTrackNext,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
@UnstableApi
private fun PlayerProgress(
    viewState: PlayerViewState,
    nowPlayingViewState: NowPlayingViewState,
    modifier: Modifier,
    onSeekTo: (Long) -> Unit,
) {

    val playerState by remember(viewState.visiblePlayerState, nowPlayingViewState.playerState) {
        derivedStateOf {
            if (nowPlayingViewState.isCurrent(viewState))
                nowPlayingViewState.playerState
            else
                viewState.visiblePlayerState
        }
    }

    if (playerState is Uninitialized) return

    val (position, duration) = playerState()?.position to playerState()?.duration

    val sliderValue by remember(position, duration) {
        derivedStateOf {
            if ((position ?: 0) > 0 && (duration ?: 0) > 0)
                position!!.toFloat().div(duration!!)
            else 0F
        }
    }

    val isSliderEnabled = playerState()?.flags?.isSliderEnabled == true

    Column(modifier = modifier) {
        Row {
            Column(
                modifier = Modifier.weight(0.5F),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = if (position != null)
                        formatTime(position)
                    else "--:--:--",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(
                modifier = Modifier.weight(0.5F),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = if (duration != null)
                        formatTime(duration)
                    else "--:--:--",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Slider(
            enabled = isSliderEnabled,
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderValue,
            valueRange = 0F..0.99F,
            onValueChange = if (duration != null) ({
                onSeekTo((it * duration.milliseconds.inWholeMilliseconds).toLong())
            }) else ({}),
        )
    }
}

private const val DURATION_ZERO: String = "00:00:00"

fun formatTime(value: Long?): String = when (value) {
    null -> ""
    0L -> DURATION_ZERO
    else -> ContentDuration.format(value.milliseconds) ?: DURATION_ZERO
}
