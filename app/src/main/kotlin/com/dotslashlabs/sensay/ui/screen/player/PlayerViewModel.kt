package com.dotslashlabs.sensay.ui.screen.player

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.PersistState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import logcat.logcat
import media.MediaPlayerFlags
import media.MediaPlayerState
import media.MediaPlaylistState
import media.PlayableMediaItem


data class PlayerViewState(
    @PersistState val bookId: Long,
    val chapterIndex: Int,

    val contextPlaylistState: Async<MediaPlaylistState> = Uninitialized,
    val contextPlayableMediaItem: Async<PlayableMediaItem> = Uninitialized,

    val visibleMediaItem: MediaItem? = contextPlayableMediaItem()?.chapter,
) : MavericksState {

    val isReady = (contextPlaylistState is Success || contextPlaylistState is Fail)

    private val visibleChapterMarker = visibleMediaItem?.mediaId?.let { mediaId ->
        contextPlayableMediaItem()?.chapterMarkers?.firstOrNull { it.mediaId == mediaId }
    }

    val visiblePlayerState: Async<MediaPlayerState> = Success(
        MediaPlayerState.Idle(
            false,
            visibleChapterMarker?.mediaId,
            visibleChapterMarker?.index,
            0,
            visibleChapterMarker?.duration,
            MediaPlayerFlags().copy(isPlayPauseEnabled = true),
        )
    )
}


class PlayerViewModel @AssistedInject constructor(
    @Assisted initialState: PlayerViewState,
    @ApplicationContext val context: Context,
) : MavericksViewModel<PlayerViewState>(initialState) {

    private val bookId = initialState.bookId
    val chapterIndex = initialState.chapterIndex

    fun setVisibleMediaItem(mediaItem: MediaItem) {
        logcat { "setVisibleMediaItem: ${mediaItem.mediaId}" }
        setState {
            copy(visibleMediaItem = mediaItem)
        }
    }

    fun loadContextMedia(
        loader: suspend (bookId: Long, chapterIndex: Int) -> Async<PlayableMediaItem>,
    ) = viewModelScope.launch {

        setState {
            copy(
                contextPlaylistState = Loading(),
                contextPlayableMediaItem = Loading(),
                visibleMediaItem = null,
            )
        }

        when (val playableMediaItem = loader(bookId, chapterIndex)) {
            is Success -> {
                val item = playableMediaItem()

                setState {
                    copy(
                        contextPlaylistState = Success(item.toPlaylistState()),
                        contextPlayableMediaItem = Success(item),
                    )
                }
            }
            is Fail -> {
                setState {
                    copy(
                        contextPlaylistState = Success(MediaPlaylistState.Empty(false)),
                        contextPlayableMediaItem = Fail(playableMediaItem.error),
                    )
                }
            }
            else -> {
                setState {
                    copy(
                        contextPlaylistState = Loading(),
                        contextPlayableMediaItem = Loading(),
                    )
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object :
        MavericksViewModelFactory<PlayerViewModel, PlayerViewState> by hiltMavericksViewModelFactory() {

        override fun initialState(viewModelContext: ViewModelContext): PlayerViewState {
            return PlayerViewState(
                bookId = viewModelContext.args<Bundle>()
                    .getLong("bookId", 0L),
                chapterIndex = viewModelContext.args<Bundle>()
                    .getInt("chapterIndex", 0),
            )
        }
    }
}
