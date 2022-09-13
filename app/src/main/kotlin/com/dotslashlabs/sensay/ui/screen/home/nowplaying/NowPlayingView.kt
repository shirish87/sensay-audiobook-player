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
import androidx.compose.ui.tooling.preview.Preview
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
import com.dotslashlabs.sensay.util.PlayerState
import data.util.ContentDuration

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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
    ) {
        state.bookProgressFraction?.let { bookProgress ->
            LinearProgressIndicator(progress = bookProgress)
        }

        Row {
            Column(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .clickable { onClick(nowPlayingBook.bookId) }
                    .padding(16.dp)
                    .weight(0.75F),
            ) {
                nowPlayingBook.series?.let { series ->
                    if (series != nowPlayingBook.bookTitle) {
                        Text(
                            text = series,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = nowPlayingBook.bookTitle.trim(),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = nowPlayingBook.chapterTitle.trim(),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall,
                )
                nowPlayingBook.author?.let { author ->
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = author,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        if (state.isPlaying) {
                            actions.pause()
                        } else {
                            actions.play()
                        }
                    }
                    .padding(16.dp)
                    .weight(0.2F, fill = true)
                    .align(Alignment.CenterVertically),
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
                    bookTitle = "Book Title",
                    chapterTitle = "Chapter Title",
                    author = "Author",
                    series = null,
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
