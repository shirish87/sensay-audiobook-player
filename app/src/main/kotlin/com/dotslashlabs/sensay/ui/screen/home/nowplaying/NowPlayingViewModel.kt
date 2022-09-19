package com.dotslashlabs.sensay.ui.screen.home.nowplaying

import android.content.Context
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.common.MediaSessionQueue
import com.dotslashlabs.sensay.ui.screen.common.BasePlayerViewModel
import com.dotslashlabs.sensay.util.PlayerState
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import logcat.logcat

data class NowPlayingViewState(
    val isLoading: Boolean = false,
    val data: Async<Pair<BookProgressWithDuration, PlayerState>> = Uninitialized,
) : MavericksState {

    val nowPlayingBook = data()?.first
    private val playerState = data()?.second

    private val bookProgressMs: Long? = nowPlayingBook?.let {
        val chapterPosition = playerState?.position ?: -1

        if (chapterPosition >= 0) {
            it.chapterStart.ms + chapterPosition
        } else null
    }

    val bookProgressFraction: Float? = nowPlayingBook?.let {
        bookProgressMs?.toFloat()?.div(maxOf(1, it.bookDuration.ms))
    }

    val isPlaying = (playerState?.isPlaying == true)
}

interface NowPlayingViewActions {
    fun play(): Unit?
    fun pause(): Unit?
    fun attachPlayer(context: Context)
    fun detachPlayer()
}

class NowPlayingViewModel @AssistedInject constructor(
    @Assisted state: NowPlayingViewState,
    private val configStore: ConfigStore,
    private val mediaSessionQueue: MediaSessionQueue,
) : BasePlayerViewModel<NowPlayingViewState>(state), NowPlayingViewActions {

    private var job: Job? = null

    override fun onCleared() {
        super.onCleared()

        detachPlayer()
    }

    override fun attachPlayer(context: Context) {
        logcat { "attachPlayer" }

        setState { copy(isLoading = true) }
        attach(context) { _, _, serviceEvents ->
            setState { copy(isLoading = false) }

            job?.cancel()
            job = serviceEvents?.execute(retainValue = NowPlayingViewState::data) {
                val playerState = (it() as? PlayerState)
                val media = playerState?.mediaId?.let { mediaId ->
                    mediaSessionQueue.getMedia(mediaId)
                }

                if (media == null) {
                    copy(data = Uninitialized)
                } else {
                    copy(data = Success(media to playerState))
                }
            }
        }
    }

    override fun detachPlayer() {
        logcat { "detachPlayer" }
        detach()

        job?.cancel()
        job = null
    }

    override fun play() = player?.play()
    override fun pause() = player?.pause()

    suspend fun getLastPlayedBookId() = configStore.getLastPlayedBookId().first()

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<NowPlayingViewModel, NowPlayingViewState> {
        override fun create(state: NowPlayingViewState): NowPlayingViewModel
    }

    companion object : MavericksViewModelFactory<NowPlayingViewModel, NowPlayingViewState>
    by hiltMavericksViewModelFactory()
}
