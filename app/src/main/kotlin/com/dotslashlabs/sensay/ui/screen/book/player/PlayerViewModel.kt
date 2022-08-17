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
import data.entity.Chapter
import data.util.ContentDuration
import kotlin.time.Duration.Companion.milliseconds

data class PlayerViewState(
    @PersistState val bookId: Long,
    val bookProgressWithChapters: Async<BookProgressWithBookAndChapters> = Uninitialized,
    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
    val selectedChapterId: Async<Long> = Uninitialized,
    val sliderPosition: Float = 0F,
) : MavericksState {
    constructor(arguments: Bundle) : this(bookId = arguments.getString("bookId")!!.toLong())

    companion object {
        const val DURATION_ZERO: String = "00:00:00"
    }

    val bookProgress = (bookProgressWithChapters as? Success)?.invoke()
    val book: Book? = bookProgress?.book
    val chapters: List<Chapter> = bookProgress?.chapters ?: emptyList()
    val coverUri: Uri? = book?.coverUri

    val selectedChapter: Chapter? = selectedChapterId()?.let {
        chapters.find { c -> c.chapterId == it }
    }

    private val connState = playbackConnectionState()

    val isConnected = (connState?.isConnected == true)

    val isPlaying = (connState?.isPlaying == true)

    val isCurrentBook = (bookId == connState?.currentBookId)

    val isPreparingCurrentBook = (bookId == connState?.preparingBookId)

    val isCurrentBookPlaying = (isCurrentBook && isPlaying)

    val isSelectedChapterCurrent =
        (bookProgress != null && bookProgress.chapter.chapterId == selectedChapterId())

    val progressPair: Pair<Long?, Long?> = if (isCurrentBook) {
        if (isSelectedChapterCurrent) {
            (connState?.currentPosition ?: 0L) to (connState?.duration ?: 0L)
        } else (null as Long? to null as Long?)
    } else if (isSelectedChapterCurrent) {
        (bookProgress?.chapterPositionMs ?: 0L) to (bookProgress?.chapterDurationMs ?: 0L)
    } else if (selectedChapter != null) {
        (0L to selectedChapter.duration.ms)
    } else (null as Long? to null as Long?)


    fun formatTime(value: Long?): String = when (value) {
        null -> ""
        0L -> DURATION_ZERO
        else -> ContentDuration.format(value.milliseconds) ?: DURATION_ZERO
    }
}

interface PlayerActions {
    fun setSelectedChapterId(chapterId: Long)
    fun setSliderPosition(position: Float)
}

class PlayerViewModel @AssistedInject constructor(
    @Assisted private val state: PlayerViewState,
    private val playbackConnection: PlaybackConnection,
    store: SensayStore,
) : MavericksViewModel<PlayerViewState>(state), PlayerActions {

    init {
        store.bookProgressWithBookAndChapters(state.bookId)
            .execute(retainValue = PlayerViewState::bookProgressWithChapters) {
                var newState = copy(bookProgressWithChapters = it)

                if (it is Success && newState.selectedChapterId is Uninitialized) {
                    newState = newState.copy(selectedChapterId = Success(it().chapter.chapterId))
                }

                newState
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

        onEach(PlayerViewState::progressPair) { (position, duration) ->
            setState {
                copy(
                    sliderPosition = if ((position ?: 0) > 0 && (duration ?: 0) > 0)
                        position!!.toFloat().div(duration!!)
                    else this.sliderPosition,
                )
            }
        }
    }

    override fun setSelectedChapterId(chapterId: Long) = setState {
        copy(selectedChapterId = Success(chapterId))
    }

    override fun setSliderPosition(position: Float) = setState {
        copy(sliderPosition = position)
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object : MavericksViewModelFactory<PlayerViewModel, PlayerViewState>
    by hiltMavericksViewModelFactory()
}
