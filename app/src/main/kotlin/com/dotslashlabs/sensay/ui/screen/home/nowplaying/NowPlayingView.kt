package com.dotslashlabs.sensay.ui.screen.home.nowplaying

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.util.PlayerState
import com.dotslashlabs.sensay.util.joinWith
import com.google.accompanist.flowlayout.FlowRow
import data.util.ContentDuration
import kotlin.text.Typography.bullet
import kotlin.text.Typography.ellipsis

@Composable
fun NowPlayingView(
    navHostController: NavHostController,
    backStackEntry: NavBackStackEntry,
    modifier: Modifier,
) {
    val viewModel: NowPlayingViewModel = mavericksViewModel(backStackEntry)
    val state by viewModel.collectAsState()

    val context = LocalContext.current

    DisposableEffect(viewModel, context) {
        viewModel.attachPlayer(context)

        onDispose {
            viewModel.detachPlayer()
        }
    }

    NowPlayingViewContent(viewModel, state, modifier, onClick = { bookId ->
        val deepLink = SensayScreen.getUriString(
            Destination.Player.useRoute(bookId)
        ).toUri()

        navHostController.navigate(deepLink)
    })
}

@Composable
fun NowPlayingViewContent(
    actions: NowPlayingViewActions,
    state: NowPlayingViewState,
    modifier: Modifier,
    onClick: (bookId: Long) -> Unit,
) {

    val nowPlayingBook = state.nowPlayingBook ?: return
    val maxHeight = resolveContentMaxHeight(nowPlayingBook)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
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
                        .weight(0.2F, fill = true)
                        .fillMaxHeight()
                        .clickable { onClick(nowPlayingBook.bookId) }
                        .padding(16.dp),
                ) {
                    CoverImage(
                        coverUri = coverUri,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .weight(0.75F, fill = true)
                    .fillMaxHeight()
                    .clickable { onClick(nowPlayingBook.bookId) }
                    .padding(16.dp),
            ) {

                BookAuthorAndSeries(
                    nowPlayingBook,
                    style = MaterialTheme.typography.labelSmall,
                )

                BookTitleAndChapter(nowPlayingBook)
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(0.25F, fill = true)
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
            LinearProgressIndicator(progress = bookProgress)
        }
    }
}

@Composable
fun BookAuthorAndSeries(
    bookProgressWithDuration: BookProgressWithDuration,
    maxLength: Int = 40,
    style: TextStyle = MaterialTheme.typography.labelSmall,
) {

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 4.dp,
    ) {
        listOfNotNull(
            bookProgressWithDuration.author,
            if (bookProgressWithDuration.series == bookProgressWithDuration.bookTitle)
                null
            else bookProgressWithDuration.series,
        )
            .joinWith("$bullet")
            .forEach { text ->
                Text(
                    text = text.run {
                        if (length > maxLength)
                            "${substring(0, maxLength)}$ellipsis"
                        else this
                    },
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
fun BookTitleAndChapter(
    bookProgressWithDuration: BookProgressWithDuration,
) {

    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = bookProgressWithDuration.bookTitle.trim(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall,
    )
    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = bookProgressWithDuration.chapterTitle.trim(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall,
    )
}


fun resolveContentMaxHeight(
    bookProgressWithDuration: BookProgressWithDuration,
    heightLow: Dp = 96.dp,
    heightHigh: Dp = 120.dp,
    charThreshold: Int = 150,
): Dp {

    return bookProgressWithDuration.run {
        val contentLength = listOfNotNull(
            author,
            series,
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
        actions = object : NowPlayingViewActions {
            override fun play(): Unit? {
                TODO("Not yet implemented")
            }

            override fun pause(): Unit? {
                TODO("Not yet implemented")
            }

            override fun attachPlayer(context: Context) {
                TODO("Not yet implemented")
            }

            override fun detachPlayer() {
                TODO("Not yet implemented")
            }
        },
        state = NowPlayingViewState(
            data = Success(
                BookProgressWithDuration(
                    mediaId = "0",
                    bookProgressId = 0L,
                    bookId = 0L,
                    chapterId = 0L,
                    bookTitle = List(1024) { "Book Title" }.joinToString(" "),
                    chapterTitle = List(1024) { "Chapter Title" }.joinToString(" "),
                    author = List(1024) { "Author" }.joinToString(" "),
                    series = List(1024) { "Series" }.joinToString(" "),
                    coverUri = null,
                    totalChapters = 10,
                    bookChapterStart = ContentDuration.ms(0L),
                    bookDuration = ContentDuration.ms(10_000L),
                ) to PlayerState(
                    isPlaying = false,
                )
            )
        ),
        modifier = Modifier,
        onClick = {},
    )
}
