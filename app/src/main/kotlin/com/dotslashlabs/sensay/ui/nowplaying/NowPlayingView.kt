package com.dotslashlabs.sensay.ui.nowplaying

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.screen.player.formatTime
import compose.icons.TablerIcons
import compose.icons.tablericons.PlayerPauseFilled
import compose.icons.tablericons.PlayerPlayFilled


@Composable
@UnstableApi
fun NowPlayingView(
    modifier: Modifier = Modifier,
    onNavigate: (extras: Bundle) -> Unit,
) {

    val nowPlayingViewModel: NowPlayingViewModel = mavericksActivityViewModel()
    val nowPlayingViewState by nowPlayingViewModel.collectAsState()

    if (!nowPlayingViewState.isPlayerAttached) return

    when (nowPlayingViewState.playerState) {
        is Success -> {
            val playerState = nowPlayingViewState.playerState()

            val item by remember(nowPlayingViewState.playerState, nowPlayingViewState.playlistState) {
                derivedStateOf {
                    playerState?.currentMediaId?.let {
                        nowPlayingViewState.playlistState()
                            ?.mediaItems
                            ?.firstOrNull { item -> item.mediaId == it }
                    }
                }
            }

            val mediaItem = item ?: return
            val context = LocalContext.current

            Surface(
                modifier = modifier
                    .systemGesturesPadding()
                    .shadow(6.dp, shape = RoundedCornerShape(6.dp), clip = true)
                    .clickable { mediaItem.mediaMetadata.extras?.let(onNavigate) },
                tonalElevation = NavigationBarDefaults.Elevation,
            ) {

                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    AsyncImage(
                        modifier = Modifier.fillMaxHeight(),
                        model = ImageRequest.Builder(context)
                            .data(mediaItem.mediaMetadata.artworkUri ?: mediaItem.mediaMetadata.artworkData)
                            .diskCacheKey(mediaItem.mediaId)
                            .build(),
                        contentDescription = mediaItem.mediaMetadata.title?.let { "" + it },
                    )

                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {

                        Text(
                            modifier = Modifier.wrapContentWidth(),
                            text = listOfNotNull(
                                mediaItem.mediaMetadata.compilation
                                    ?: mediaItem.mediaMetadata.albumTitle,
                                mediaItem.mediaMetadata.title,
                            ).joinToString(" - ") { it.toString().trim() },
                            style = MaterialTheme.typography.titleSmall,
                            lineHeight = 1.2.em,
                            maxLines = 3,
                        )

                        Text(
                            text = "${playerState?.position?.let { formatTime(it) }} / ${playerState?.duration?.let { formatTime(it) }}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    PlayerControls(
                        nowPlayingViewModel,
                        modifier = Modifier.weight(0.2f).fillMaxHeight(),
                    )
                }

            }
        }
        else -> {}
    }
}

@Composable
@UnstableApi
fun PlayerControls(
    viewModel: NowPlayingViewModel,
    modifier: Modifier = Modifier,
) {

    val viewState by viewModel.collectAsState()

    val playerState = viewState.playerState
    if (playerState is Uninitialized) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        if (playerState is Loading || playerState is Fail) {
            Box(Modifier.fillMaxSize()) {
                when (playerState) {
                    is Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is Fail -> Text(
                        text = "Error: ${playerState.error.message}",
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> Unit
                }
            }

            return
        }

        val mediaPlayerState = playerState() ?: return

        IconButton(
            modifier = Modifier.fillMaxSize(),
            enabled = mediaPlayerState.isEnabled,
            onClick = {
                if (mediaPlayerState.isPlaying) {
                    viewModel.pause()
                } else {
                    viewModel.play()
                }
            },
        ) {
            Icon(
                imageVector = if (mediaPlayerState.isPlaying)
                    TablerIcons.PlayerPauseFilled
                else TablerIcons.PlayerPlayFilled,
                contentDescription = null,
            )
        }
    }
}
