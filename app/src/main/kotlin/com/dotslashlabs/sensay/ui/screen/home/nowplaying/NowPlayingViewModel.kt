package com.dotslashlabs.sensay.ui.screen.home.nowplaying


import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.common.MediaSessionQueue
import com.dotslashlabs.sensay.common.PlayerHolder
import com.dotslashlabs.sensay.common.SensayPlayer
import com.dotslashlabs.sensay.util.PlayerState
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import logcat.logcat

data class NowPlayingViewState(
    val data: Async<Pair<BookProgressWithDuration, PlayerState>> = Uninitialized,
) : MavericksState {

    val nowPlayingBook = data()?.first
    private val playerState = data()?.second

    val bookProgress: Long? = nowPlayingBook?.let {
        val chapterPosition = playerState?.position ?: -1

        if (chapterPosition >= 0) {
            it.chapterStart.ms + chapterPosition
        } else null
    }

    val bookProgressFraction: Float? = nowPlayingBook?.let {
        bookProgress?.let { progress -> progress.toFloat() / it.bookDuration.ms }
    }

    val isPlaying = (playerState?.isPlaying == true)
}

interface NowPlayingViewActions {
    fun play(): Unit?
    fun pause(): Unit?
    fun subscribe()
    fun unsubscribe()
}

class NowPlayingViewModel @AssistedInject constructor(
    @Assisted private val state: NowPlayingViewState,
    private val configStore: ConfigStore,
    private val playerHolder: PlayerHolder,
    private val mediaSessionQueue: MediaSessionQueue,
) : MavericksViewModel<NowPlayingViewState>(state), NowPlayingViewActions {

    private var player: SensayPlayer? = null
    private var job: Job? = null

    override fun subscribe() {
        viewModelScope.launch {
            playerHolder.connection.collectLatest { p ->
                player = p ?: return@collectLatest

                unsubscribe()
                job = p.serviceEvents
                    .mapNotNull {
                        it.mediaId?.let { mediaId ->
                            mediaSessionQueue.getMedia(mediaId)?.let { progress ->
                                progress to it
                            }
                        }
                    }
                    .execute(retainValue = NowPlayingViewState::data) {
                        logcat { "nowPlayingBook: ${it()}" }
                        copy(data = it)
                    }
            }
        }
    }

    override fun unsubscribe() {
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
