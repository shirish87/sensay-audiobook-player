package com.dotslashlabs.sensay.ui

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
    val isPreparing: Boolean = false,
    val currentBook: Async<Book?> = Uninitialized,
) : MavericksState {

    val isConnected = (_isConnected() == true)
    val isPlaying = (_isPlaying() == true)

    fun isCurrentBook(book: Book): Boolean = (currentBook.invoke()?.bookId == book.bookId)
}

class PlaybackViewModel @AssistedInject constructor(
    @Assisted private val state: PlaybackState,
    private val playbackConnection: PlaybackConnection,
) : MavericksViewModel<PlaybackState>(state) {

    init {
        playbackConnection.isConnected.execute { copy(_isConnected = it) }
        playbackConnection.isPlaying.execute { copy(_isPlaying = it) }

        playbackConnection.currentBook.execute(retainValue = PlaybackState::currentBook) {
            copy(currentBook = it, isPreparing = false)
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
        val player = playbackConnection.player ?: return

        val book = bookProgressWithBookAndChapters.book
        if (player.currentMediaItem?.mediaId == book.bookId.toString()) return

        // preparing items takes a while, possibly due to bundle stuff, so we disable playback
        // isPreparing flag is reset when state.currentBook is updated
        setState { copy(isPreparing = true ) }

        player.apply {
            setMediaItem(
                MediaItem.Builder()
                    .setUri(book.uri)
                    .setMediaId(book.bookId.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(book.title)
                            .setArtist(book.author)
                            .setIsPlayable(true)
                            .setArtworkUri(book.coverUri)
                            .setExtras(book.toBundle())
                            .build()
                    )
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(book.uri)
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
