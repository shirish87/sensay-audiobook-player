package com.dotslashlabs.sensay.ui.screen.book.player

import android.net.Uri
import android.os.Bundle
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.service.PlaybackConnection
import com.dotslashlabs.sensay.service.PlaybackConnectionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.Book
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlin.time.Duration.Companion.milliseconds

data class PlayerViewState(
    @PersistState val bookId: Long,
    val bookProgressWithChapters: Async<BookProgressWithBookAndChapters> = Uninitialized,
    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
) : MavericksState {
    constructor(arguments: Bundle) : this(bookId = arguments.getString("bookId")!!.toLong())

    val bookProgress = (bookProgressWithChapters as? Success)?.invoke()
    val book: Book? = bookProgress?.book
    val coverUri: Uri? = book?.coverUri

    private val connState = playbackConnectionState()

    val isCurrentBook = (bookId == connState?.currentBookId)

    val isPreparingCurrentBook = (bookId == connState?.preparingBookId)

    val isPlaying = (connState?.isPlaying == true)

    val isCurrentBookPlaying = (isCurrentBook && isPlaying)

    val duration: String
        get() = ContentDuration.format(connState?.duration?.milliseconds)

    val currentPosition: String
        get() = ContentDuration.format(connState?.currentPosition?.milliseconds)
}

class PlayerViewModel @AssistedInject constructor(
    @Assisted private val state: PlayerViewState,
    private val playbackConnection: PlaybackConnection,
    store: SensayStore,
) : MavericksViewModel<PlayerViewState>(state) {

    init {
        store.bookProgressWithBookAndChapters(state.bookId)
            .execute(retainValue = PlayerViewState::bookProgressWithChapters) {
                copy(bookProgressWithChapters = it)
            }

        playbackConnection.state
            .execute(retainValue = PlayerViewState::playbackConnectionState) {
                copy(playbackConnectionState = it)
            }

        onEach(PlayerViewState::isCurrentBookPlaying) { isCurrentBookPlaying ->
            if (isCurrentBookPlaying) {
                playbackConnection.startLiveUpdates(viewModelScope)
            } else {
                playbackConnection.stopLiveUpdates()
            }
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object : MavericksViewModelFactory<PlayerViewModel, PlayerViewState>
    by hiltMavericksViewModelFactory()
}
