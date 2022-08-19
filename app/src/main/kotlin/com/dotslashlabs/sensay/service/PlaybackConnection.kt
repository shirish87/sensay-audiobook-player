package com.dotslashlabs.sensay.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Assertions
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dotslashlabs.sensay.util.PlayerState
import com.dotslashlabs.sensay.util.mediaIds
import com.dotslashlabs.sensay.util.state
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.seconds

data class PlaybackConnectionState(
    val isConnected: Boolean = false,
    val playerState: PlayerState = PlayerState(),
    val playerMediaIds: List<String> = emptyList(),
)

class PlaybackConnection constructor(private val context: Context) {

    private var _mediaController: MediaController? = null
    val player: Player?
        get() {
            Assertions.checkMainThread()

            return if (_mediaController?.isConnected == true)
                _mediaController
            else null
        }

    private val _state = MutableStateFlow(PlaybackConnectionState())
    val state: Flow<PlaybackConnectionState> = _state

    // live updates for play progress
    private val stateRecorder = PlayerStateRecorder(
        1.seconds,
        { player },
        _state,
        { player ->
            _state.value.copy(
                playerState = _state.value.playerState.copy(
                    position = player.currentPosition,
                    duration = player.duration,
                )
            )
        },
    )

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
        playerMediaIds: List<String> = _state.value.playerMediaIds,
    ): PlaybackConnectionState {
        val playerState = player?.state ?: PlayerState()

        return PlaybackConnectionState(
            isConnected = isConnected,
            playerState = playerState,
            playerMediaIds = playerMediaIds,
        )
    }

    private val playerListener = object : Player.Listener {
//        override fun onEvents(player: Player, events: Player.Events) {
//            logcat { "onEvents: ${(0 until events.size()).joinToString { events.get(it).toString() }}" }
//            _state.value = stateFromPlayer()
//        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = stateFromPlayer(playerMediaIds = player.mediaIds)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.value = stateFromPlayer()
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            _state.value = stateFromPlayer(playerMediaIds = player.mediaIds)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = stateFromPlayer()
        }
    }
}
