package com.dotslashlabs.sensay.ui

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.service.PlaybackConnection
import com.dotslashlabs.sensay.service.PlaybackConnectionState
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewState.Companion.getMediaId
import com.dotslashlabs.sensay.util.bookId
import com.dotslashlabs.sensay.util.mediaId
import com.dotslashlabs.sensay.util.toMediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.entity.BookProgressWithBookAndChapters
import logcat.logcat

data class PlaybackState(
    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
) : MavericksState {

    private val connState = playbackConnectionState()

    val isConnected = (connState?.isConnected == true)
}

interface PlaybackActions {
    var playWhenReady: Boolean

    fun prepareMediaItems(
        bookProgressWithBookAndChapters: BookProgressWithBookAndChapters,
        selectedChapterId: Long? = null,
    )

    fun seekBack(): Unit?
    fun seekForward(): Unit?
    fun seekTo(positionMs: Long): Unit?
    fun pause(): Unit?
    fun play(): Unit?
    fun setChapter(chapterId: Long): Unit?
}

class PlaybackViewModel @AssistedInject constructor(
    @Assisted private val state: PlaybackState,
    private val playbackConnection: PlaybackConnection,
) : MavericksViewModel<PlaybackState>(state), PlaybackActions {

    init {
        playbackConnection.state
            .execute(retainValue = PlaybackState::playbackConnectionState) {
                copy(playbackConnectionState = it)
            }
    }

    val player: Player?
        get() = playbackConnection.player

    override var playWhenReady: Boolean
        get() = player?.playWhenReady == true
        set(value) {
            player?.playWhenReady = value
        }

    override fun prepareMediaItems(
        bookProgressWithBookAndChapters: BookProgressWithBookAndChapters,
        selectedChapterId: Long?,
    ) {
        val player = this.player ?: return

        val bookId = bookProgressWithBookAndChapters.book.bookId
        val chapterId = selectedChapterId ?: bookProgressWithBookAndChapters.chapter.chapterId

        val mediaId = getMediaId(bookId, chapterId)
        if (mediaId == player.mediaId) return // already active

        val chapterCount = bookProgressWithBookAndChapters.chapters.size
        if (chapterCount == 0) return

        val chapterIndex = bookProgressWithBookAndChapters.chapters
            .indexOfFirst { c -> c.chapterId == chapterId }

        // chapterId not found
        logcat { "prepareMediaItems: chapterId=$chapterId chapterIndex=$chapterIndex" }
        if (chapterIndex == -1) return

        // preparing items takes a while, possibly due to bundle stuff, so we disable playback
        // isPreparing flag is reset when state.currentBook is updated
        playbackConnection.setPreparingMediaId(mediaId)

        val progress = bookProgressWithBookAndChapters.bookProgress
        val (startMediaIndex, startPositionMs) =
            if (progress.chapterId == chapterId) {
                // selected chapter has existing progress recorded
                progress.currentChapter to progress.chapterProgress.ms
            } else {
                chapterIndex to 0L
            }

        player.apply {
            if (bookId == player.bookId && player.mediaItemCount == chapterCount) {
                // required book is already loaded
                // but since mediaId doesn't match,
                // we need to switch the chapter
                seekTo(startMediaIndex, startPositionMs)
            } else {
                setMediaItems(
                    bookProgressWithBookAndChapters.toMediaItems(),
                    startMediaIndex,
                    startPositionMs,
                )

                prepare()
            }
        }
    }

    override fun seekBack() = player?.seekBack()
    override fun seekForward() = player?.seekForward()

    override fun seekTo(positionMs: Long): Unit? = player?.seekTo(positionMs)

    override fun pause() = player?.pause()

    override fun play() {
        player?.apply {
            playWhenReady = true
            play()
        }
    }

    override fun setChapter(chapterId: Long) {
        val bookId = player?.bookId ?: return
        val mediaId = getMediaId(bookId, chapterId)

        val mediaItemIndex = findMediaItemIndex { it.mediaId == mediaId }
        logcat { "setChapter: chapterId=$chapterId mediaItemIndex=$mediaItemIndex" }
        if (mediaItemIndex == -1) return

        player?.apply {
            val playerState = state.playbackConnectionState()?.playerState

            if (mediaId == playerState?.mediaId && playerState.position != null) {
                 logcat { "seekTo: mediaItemIndex=$mediaItemIndex position=${playerState.position}" }
                seekTo(mediaItemIndex, playerState.position)
            } else {
                 logcat { "seekToDefaultPosition: mediaItemIndex=$mediaItemIndex" }
                seekToDefaultPosition(mediaItemIndex)
            }
        }
    }

    private fun findMediaItemIndex(condition: (mediaItem: MediaItem) -> Boolean): Int {
        val mediaItemCount = player?.mediaItemCount ?: return -1

        return (0 until mediaItemCount).fold(-1) { acc, idx ->
            if (acc != -1) return acc

            player?.getMediaItemAt(idx)?.let {
                if (condition(it)) idx else null
            } ?: acc
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlaybackViewModel, PlaybackState> {
        override fun create(state: PlaybackState): PlaybackViewModel
    }

    companion object : MavericksViewModelFactory<PlaybackViewModel, PlaybackState>
    by hiltMavericksViewModelFactory()
}

fun BookProgressWithBookAndChapters.toMediaItems(): List<MediaItem> {
    require(chapters.isNotEmpty()) { "Chapters should exist" }

    return chapters.map {
        require(!it.isInvalid()) { "Each chapter should be valid" }

        toMediaItem(it.chapterId)
    }
}
