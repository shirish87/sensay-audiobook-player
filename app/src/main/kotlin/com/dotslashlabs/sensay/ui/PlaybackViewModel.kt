package com.dotslashlabs.sensay.ui

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.service.PlaybackConnection
import com.dotslashlabs.sensay.service.PlaybackConnectionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.entity.BookProgressWithBookAndChapters

data class PlaybackState(
    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
) : MavericksState {

    private val connState = playbackConnectionState()

    val isConnected = (connState?.isConnected == true)
}

class PlaybackViewModel @AssistedInject constructor(
    @Assisted private val state: PlaybackState,
    private val playbackConnection: PlaybackConnection,
) : MavericksViewModel<PlaybackState>(state) {

    init {
        playbackConnection.state
            .execute(retainValue = PlaybackState::playbackConnectionState) {
                copy(playbackConnectionState = it)
            }
    }

    val player: Player?
        get() = playbackConnection.player

    var playWhenReady: Boolean
        get() = player?.playWhenReady == true
        set(value) {
            player?.playWhenReady = value
        }

    fun prepareMediaItems(bookProgressWithBookAndChapters: BookProgressWithBookAndChapters) {
        val player = this.player ?: return

        val book = bookProgressWithBookAndChapters.book
        if (player.currentMediaItem?.mediaId == book.bookId.toString()) return

        // preparing items takes a while, possibly due to bundle stuff, so we disable playback
        // isPreparing flag is reset when state.currentBook is updated
        playbackConnection.setPreparingBookId(book.bookId)

        val startMediaIndex =
            bookProgressWithBookAndChapters.chapters.indexOf(bookProgressWithBookAndChapters.chapter)
        val startPositionMs = bookProgressWithBookAndChapters.startPositionMs

        player.apply {
            setMediaItems(
                bookProgressWithBookAndChapters.toMediaItems(),
                startMediaIndex,
                startPositionMs,
            )
            prepare()
        }
    }

    fun seekBack() = player?.seekBack()
    fun seekForward() = player?.seekForward()
    fun pause() = player?.pause()
    fun play() = player?.play()

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlaybackViewModel, PlaybackState> {
        override fun create(state: PlaybackState): PlaybackViewModel
    }

    companion object : MavericksViewModelFactory<PlaybackViewModel, PlaybackState>
    by hiltMavericksViewModelFactory()
}

fun BookProgressWithBookAndChapters.toMediaItems(): List<MediaItem> {
    if (chapters.isEmpty()) {
        return listOf(
            MediaItem.Builder()
                .setMediaId(book.bookId.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setExtras(
                            bundleOf(
                                PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
                            )
                        )
                        .build()
                )
                .build()
        )
    }

    return chapters.mapNotNull {
        if (it.start == null || it.end == null) return@mapNotNull null

        MediaItem.Builder()
            .setMediaId(it.chapterId.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(
                        bundleOf(
                            PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
                            PlaybackConnection.BUNDLE_KEY_CHAPTER_ID to it.chapterId,
                        )
                    )
                    .build()
            )
            .build()
    }
}
