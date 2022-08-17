package com.dotslashlabs.sensay.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Assertions
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.seconds

data class PlaybackConnectionState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentBookId: Long? = null,
    val currentChapterId: Long? = null,
    val currentPosition: Long? = null,
    val duration: Long? = null,
    val preparingBookId: Long? = null,
)

class PlaybackConnection constructor(private val context: Context) {

    companion object {
        const val BUNDLE_KEY_BOOK_ID = "bookId"
        const val BUNDLE_KEY_CHAPTER_ID = "chapterId"

        fun fromExtras(extras: Bundle?, key: String): Long? {
            if (extras == null) return null

            val value = extras.getLong(key, Long.MIN_VALUE)
            return if (value == Long.MIN_VALUE) null else value
        }
    }

    private var _mediaController: MediaController? = null
    val player: Player?
        get() {
            Assertions.checkMainThread()

            return if (_mediaController?.isConnected == true)
                _mediaController
            else null
        }

    private val _state = MutableStateFlow(PlaybackConnectionState())
    val state: StateFlow<PlaybackConnectionState> = _state

    // live updates for play progress
    private val stateRecorder = PlayerStateRecorder(
        1.seconds,
        { player },
        _state,
        { player ->
            _state.value.copy(
                currentPosition = player.currentPosition,
                duration = player.duration,
            )
        },
    )

    fun setPreparingBookId(bookId: Long?) {
        _state.value = _state.value.copy(
            preparingBookId = bookId,
        )
    }

    fun start() {
        val controllerFuture = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()

        Futures.addCallback(controllerFuture, object : FutureCallback<MediaController> {
            override fun onSuccess(result: MediaController?) {
                _mediaController = result
                _mediaController?.addListener(playerListener)

                _state.value = stateFromPlayer(
                    isConnected = (result?.isConnected == true),
                )
            }

            override fun onFailure(t: Throwable) {
                release()
            }
        }, context.mainExecutor)
    }

    fun stop() = release()

    fun startLiveUpdates(scope: CoroutineScope) = stateRecorder.startStateRecorder(scope)

    fun stopLiveUpdates() = stateRecorder.stopStateRecorder()

    private fun release() {
        stateRecorder.release()
        _state.value = PlaybackConnectionState()
        _mediaController?.removeListener(playerListener)
        _mediaController = null
    }

    private fun stateFromPlayer(
        isConnected: Boolean = _state.value.isConnected,
    ): PlaybackConnectionState {
        val bookId = fromExtras(player?.mediaMetadata?.extras, BUNDLE_KEY_BOOK_ID)
        val chapterId = fromExtras(player?.mediaMetadata?.extras, BUNDLE_KEY_CHAPTER_ID)

        return PlaybackConnectionState(
            isConnected = isConnected,
            isPlaying = (player?.isPlaying == true),
            isLoading = (player?.isLoading == true),
            currentBookId = bookId,
            currentChapterId = chapterId,
            currentPosition = player?.currentPosition,
            duration = player?.duration,
            preparingBookId = if (bookId != state.value.preparingBookId)
                state.value.preparingBookId
            else null,
        )
    }

    private val playerListener = object : Player.Listener {
//        override fun onEvents(player: Player, events: Player.Events) {
//            logcat { "onEvents: ${(0 until events.size()).joinToString { events.get(it).toString() }}" }
//            _state.value = stateFromPlayer()
//        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            _state.value = stateFromPlayer()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _state.value = stateFromPlayer()
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            _state.value = stateFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.value = stateFromPlayer()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = stateFromPlayer()
        }
    }
}
