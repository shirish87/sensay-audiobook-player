package com.dotslashlabs.sensay.ui.screen.home.nowplaying

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.ui.PlayerAppViewActions
import com.dotslashlabs.sensay.ui.PlayerAppViewModel
import com.dotslashlabs.sensay.ui.PlayerAppViewState
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.player.Media
import com.dotslashlabs.sensay.util.PlayerState
import com.dotslashlabs.sensay.util.joinWith
import com.google.common.util.concurrent.ListenableFuture
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlin.text.Typography.bullet
import kotlin.text.Typography.ellipsis

@Composable
fun NowPlayingView(
    @Suppress("UNUSED_PARAMETER")
    lifecycleOwner: LifecycleOwner,
    onNavigate: (Uri) -> Unit,
    modifier: Modifier,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
) {

    val viewModel: PlayerAppViewModel = mavericksActivityViewModel()
    val state by viewModel.collectAsState()

    NowPlayingViewContent(viewModel, state, modifier, tonalElevation, onClick = { bookId ->
        val deepLink = SensayScreen.getUriString(
            Destination.Player.useRoute(bookId)
        ).toUri()

        onNavigate(deepLink)
    })
}

@Composable
fun NowPlayingViewContent(
    actions: PlayerAppViewActions,
    state: PlayerAppViewState,
    modifier: Modifier,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    onClick: (bookId: Long) -> Unit,
) {

    val nowPlayingBook = state.nowPlayingBook ?: return
    val maxHeight = resolveContentMaxHeight(nowPlayingBook)

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = tonalElevation,
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxHeight = maxHeight),
            ) {

                nowPlayingBook.coverUri?.let { coverUri ->
                    Column(
                        modifier = Modifier
                            .weight(0.2F)
                            .fillMaxHeight()
                            .clickable { onClick(nowPlayingBook.bookId) }
                            .padding(vertical = 16.dp, horizontal = 6.dp),
                    ) {
                        CoverImage(
                            coverUri = coverUri,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(0.75F)
                        .fillMaxHeight()
                        .clickable { onClick(nowPlayingBook.bookId) }
                        .padding(16.dp),
                ) {

                    BookTitleAndChapterFlow(nowPlayingBook)
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(0.25F)
                        .fillMaxHeight()
                        .clickable {
                            if (state.isPlaying) {
                                actions.pause()
                            } else {
                                actions.play()
                            }
                        },
                ) {
                    Icon(
                        modifier = Modifier,
                        imageVector = if (state.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = null,
                    )
                }
            }

            state.bookProgressFraction?.let { bookProgress ->
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = bookProgress,
                )
            }
        }
    }
}

@Composable
@ExperimentalLayoutApi
fun BookAuthorAndSeries(
    author: String?,
    series: String?,
    bookTitle: String,
    modifier: Modifier = Modifier,
    maxLengthEach: Int = 40,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {

    if (author == null && series == null) return

    FlowRow(
        modifier = modifier.fillMaxWidth(0.95F),
        horizontalArrangement = horizontalArrangement,
    ) {
        listOfNotNull(
            author,
            if (series == bookTitle)
                null
            else series,
        )
            .joinWith("$bullet")
            .forEach { text ->
                Text(
                    text = text.run {
                        if (length > maxLengthEach)
                            "${substring(0, maxLengthEach)}$ellipsis"
                        else this
                    },
                    style = style,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
fun BookTitleAndChapterFlow(
    nowPlayingBook: BookProgressWithDuration,
    modifier: Modifier = Modifier,
    sourceStyle: TextStyle = MaterialTheme.typography.labelSmall,
    mainStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = listOfNotNull(
                nowPlayingBook.author,
                nowPlayingBook.series,
            ).joinToString(" $bullet ") { it.trim() },
            style = sourceStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = listOfNotNull(
                nowPlayingBook.chapterTitle,
                if (nowPlayingBook.bookTitle != nowPlayingBook.series)
                    nowPlayingBook.bookTitle
                else null,
            ).joinToString(" $bullet ") { it.trim() },
            style = mainStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BookTitleAndChapter(
    bookTitle: String,
    chapterTitle: String?,
    bookTitleMaxLines: Int = 2,
    chapterTitleMaxLines: Int = 1,
    bookTitleStyle: TextStyle = MaterialTheme.typography.titleSmall,
    chapterTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {

    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = bookTitle.trim(),
        maxLines = bookTitleMaxLines,
        overflow = TextOverflow.Ellipsis,
        style = bookTitleStyle,
    )

    if (chapterTitle != null) {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = chapterTitle.trim(),
            maxLines = chapterTitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            style = chapterTitleStyle,
        )
    }
}

@Composable
fun BookChapter(
    chapterTitle: String?,
    chapterTitleMaxLines: Int = 1,
    chapterTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {

    if (chapterTitle != null) {
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = chapterTitle.trim(),
            maxLines = chapterTitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            style = chapterTitleStyle,
        )
    }
}

@Composable
fun BookTitle(
    bookTitle: String,
    bookTitleMaxLines: Int = 2,
    bookTitleStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {

    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = bookTitle.trim(),
        maxLines = bookTitleMaxLines,
        overflow = TextOverflow.Ellipsis,
        style = bookTitleStyle,
    )
}

fun resolveContentMaxHeight(
    bookProgressWithDuration: BookProgressWithDuration,
    heightLow: Dp = 96.dp,
    heightHigh: Dp = 110.dp,
    charThreshold: Int = 80,
): Dp {

    return bookProgressWithDuration.run {
        val contentLength = listOfNotNull(
            author,
            if (series != bookTitle) series else null,
            bookTitle,
            chapterTitle,
        ).joinToString(" ").length

        if (contentLength > charThreshold)
            heightHigh
        else heightLow
    }
}

@Composable
@Preview
fun NowPlayingViewContentPreview() {
    NowPlayingViewContent(
        actions = object : PlayerAppViewActions {
            override fun play(bookProgressWithChapters: BookProgressWithBookAndChapters): Unit? {
                TODO("Not yet implemented")
            }

            override fun play(): Unit? {
                TODO("Not yet implemented")
            }

            override fun pause(): Unit? {
                TODO("Not yet implemented")
            }

            override fun seekBack(): Unit? {
                TODO("Not yet implemented")
            }

            override fun seekForward(): Unit? {
                TODO("Not yet implemented")
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long): Unit? {
                TODO("Not yet implemented")
            }

            override fun sendCustomCommand(
                command: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult>? {
                TODO("Not yet implemented")
            }

            override fun startLiveTracker(): Unit? {
                TODO("Not yet implemented")
            }

            override fun stopLiveTracker(): Unit? {
                TODO("Not yet implemented")
            }

            override fun attachPlayer(context: Context) {
                TODO("Not yet implemented")
            }

            override fun detachPlayer() {
                TODO("Not yet implemented")
            }

            override suspend fun prepareMediaItems(
                selectedMedia: Media,
                mediaList: List<Media>,
                mediaIds: List<String>,
                playerMediaIds: List<String>
            ): Unit? {
                TODO("Not yet implemented")
            }
        },
        state = PlayerAppViewState(
            nowPlayingBook = BookProgressWithDuration(
                mediaId = "0",
                bookProgressId = 0L,
                bookId = 0L,
                chapterId = 0L,
                bookTitle = List(1024) { "Moontide Quartet #1: Mage's Blood" }.joinToString(" "),
                chapterTitle = List(1024) { "Chapter 001" }.joinToString(" "),
                author = List(1024) { "David Hair" }.joinToString(" "),
                series = List(1024) { "Series" }.joinToString(" "),
                coverUri = null,
                totalChapters = 10,
                bookChapterStart = ContentDuration.ms(0L),
                bookDuration = ContentDuration.ms(10_000L),
            ),
            playerState = Success(
                PlayerState(isPlaying = false),
            ),
        ),
        modifier = Modifier,
        onClick = {},
    )
}
