package com.dotslashlabs.sensay.ui

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.service.PlaybackConnection
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.entity.Book
import data.entity.BookProgressWithBookAndChapters

data class PlaybackState(
    private val _isConnected: Async<Boolean> = Uninitialized,
    private val _isPlaying: Async<Boolean> = Uninitialized,
    val isPreparingBookId: Long? = null,
    val currentBookId: Async<Long?> = Uninitialized,
) : MavericksState {

    val isConnected = (_isConnected() == true)
    val isPlaying = (_isPlaying() == true)
    val isPreparing = (isPreparingBookId != null && isPreparingBookId != currentBookId())

    fun isCurrentBook(book: Book): Boolean = (book.bookId == currentBookId())
}

class PlaybackViewModel @AssistedInject constructor(
    @Assisted private val state: PlaybackState,
    private val playbackConnection: PlaybackConnection,
) : MavericksViewModel<PlaybackState>(state) {

    init {
        playbackConnection.isConnected.execute { copy(_isConnected = it) }
        playbackConnection.isPlaying.execute { copy(_isPlaying = it) }

        playbackConnection.currentBookId
            .execute(retainValue = PlaybackState::currentBookId) {
                copy(currentBookId = it)
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
        setState { copy(isPreparingBookId = book.bookId) }

        player.apply {
            setMediaItem(
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
