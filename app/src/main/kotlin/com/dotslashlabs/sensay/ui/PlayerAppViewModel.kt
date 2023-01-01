package com.dotslashlabs.sensay.ui

import android.content.Context
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.common.MediaSessionQueue
import com.dotslashlabs.sensay.ui.screen.common.BasePlayerViewModel
import com.dotslashlabs.sensay.ui.screen.player.Media
import com.dotslashlabs.sensay.util.PlayerState
import com.google.common.util.concurrent.ListenableFuture
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import logcat.logcat
import okhttp3.internal.toHexString
import kotlin.time.Duration.Companion.milliseconds

data class PlayerAppViewState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val playerState: Async<PlayerState> = Uninitialized,
    val nowPlayingBook: BookProgressWithDuration? = null,
) : MavericksState {

    companion object {
        private const val DURATION_ZERO: String = "00:00:00"

        fun getMediaId(bookId: Long, chapterId: Long) = "books/$bookId/chapters/$chapterId"

        fun formatTime(value: Long?): String = when (value) {
            null -> ""
            0L -> DURATION_ZERO
            else -> ContentDuration.format(value.milliseconds) ?: DURATION_ZERO
        }
    }

    private val playState = playerState()

    val isPlaying: Boolean = (playState?.isPlaying == true)

    private val bookProgressMs: Long? = nowPlayingBook?.let {
        val chapterPosition = playState?.position ?: -1

        if (chapterPosition >= 0) {
            it.chapterStart.ms + chapterPosition
        } else null
    }

    val bookProgressFraction: Float? = nowPlayingBook?.let {
        bookProgressMs?.toFloat()?.div(maxOf(1, it.bookDuration.ms))
    }

    init {
        logcat { "PlayerAppViewState: init ${hashCode().toHexString()}" }
    }
}

interface PlayerAppViewActions {
    fun attachPlayer(context: Context): Unit?
    fun detachPlayer(): Unit?

    fun prepareMediaItems(
        selectedMedia: Media,
        mediaList: List<Media>,
        mediaIds: List<String>,
        playerMediaIds: List<String>,
    ): Unit?

    fun play(bookProgressWithChapters: BookProgressWithBookAndChapters): Unit?
    fun play(): Unit?
    fun pause(): Unit?

    fun seekBack(): Unit?
    fun seekForward(): Unit?
    fun seekTo(mediaItemIndex: Int, positionMs: Long): Unit?

    fun sendCustomCommand(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult>?
    fun startLiveTracker(): Unit?
    fun stopLiveTracker(): Unit?
}

class PlayerAppViewModel @AssistedInject constructor(
    @Assisted state: PlayerAppViewState,
    private val mediaSessionQueue: MediaSessionQueue,
    private val configStore: ConfigStore,
) : BasePlayerViewModel<PlayerAppViewState>(state), PlayerAppViewActions {

    private var job: Job? = null

    init {
        onAsync(PlayerAppViewState::playerState) { playerState ->
            val media = playerState.mediaId?.let(mediaSessionQueue::getMedia)
            setState { copy(nowPlayingBook = media) }
        }

        logcat { "PlayerAppViewModel: init ${hashCode().toHexString()}" }
    }

    override fun onCleared() {
        super.onCleared()

        detachPlayer()
    }

    override fun attachPlayer(context: Context) {
        setState { copy(isLoading = true) }
        attach(context) { err, _, serviceEvents ->
            logcat { "attachPlayer: ${player?.hashCode()?.toHexString()}" }
            setState { copy(isLoading = false, error = err?.message) }

            job?.cancel()
            job = serviceEvents
                ?.execute(retainValue = PlayerAppViewState::playerState) { state ->
                    copy(playerState = state)
                }
        }
    }

    override fun detachPlayer() {
        logcat { "detachPlayer: ${player?.hashCode()?.toHexString()}" }
        setState { copy(isLoading = false, error = null) }
        detach()

        job?.cancel()
        job = null
    }

    override fun startLiveTracker(): Unit? = player?.startLiveTracker(viewModelScope)
    override fun stopLiveTracker(): Unit? = player?.stopLiveTracker()

    override fun seekBack() = player?.seekBack()
    override fun seekForward() = player?.seekForward()

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) =
        player?.seekTo(mediaItemIndex, positionMs)

    override fun pause() = player?.pause()
    override fun play() = player?.play()

    override fun play(bookProgressWithChapters: BookProgressWithBookAndChapters) {
        viewModelScope.launch(Dispatchers.Main) {
            if (prepareMediaItems(bookProgressWithChapters)) {
                player?.playWhenReady = true
                play()
            }
        }
    }

    private suspend fun prepareMediaItems(
        bookProgressWithChapters: BookProgressWithBookAndChapters,
    ): Boolean {
        val player = player ?: return false

        val (bookProgress, book, _, chapters) = bookProgressWithChapters
        val selectedMediaId =
            PlayerAppViewState.getMediaId(bookProgress.bookId, bookProgress.chapterId)

        val connState = player.playerEvents.firstOrNull()
        if (connState?.playerState?.mediaId == selectedMediaId) {
            // already prepared
            return true
        }

        val mediaList = BookProgressWithDuration.fromBookAndChapters(
            bookProgress,
            book,
            chapters,
        )

        val selectedMedia = mediaList.first { it.mediaId == selectedMediaId }
        val mediaIds = mediaList.map { it.mediaId }
        val playerMediaIds = connState?.playerMediaIds ?: emptyList()

        prepareMediaItems(
            selectedMedia,
            mediaList,
            mediaIds,
            playerMediaIds,
        )

        return true
    }

    override fun sendCustomCommand(
        command: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult>? =
        (player?.player as? MediaController)?.sendCustomCommand(command, args)

    suspend fun getLastPlayedBookId() = configStore.getLastPlayedBookId().first()

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerAppViewModel, PlayerAppViewState> {
        override fun create(state: PlayerAppViewState): PlayerAppViewModel
    }

    companion object : MavericksViewModelFactory<PlayerAppViewModel, PlayerAppViewState>
    by hiltMavericksViewModelFactory()
}
