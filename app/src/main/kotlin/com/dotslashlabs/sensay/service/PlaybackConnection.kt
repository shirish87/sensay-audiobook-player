package com.dotslashlabs.sensay.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.Assertions
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackConnectionState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentBookId: Long? = null,
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

    fun start() {
        val controllerFuture = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()

        Futures.addCallback(controllerFuture, object : FutureCallback<MediaController> {
            override fun onSuccess(result: MediaController?) {
                _mediaController = result
                _mediaController?.addListener(playerListener)
                _state.value = _state.value.copy(isConnected = (result?.isConnected == true))
            }

            override fun onFailure(t: Throwable) {
                release()
            }
        }, context.mainExecutor)
    }

    fun stop() = release()

    private fun release() {
        _state.value = _state.value.copy(isConnected = false)
        _mediaController?.removeListener(playerListener)
        _mediaController = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            player?.mediaMetadata?.let { register(it) }
        }

        private fun register(mediaMetadata: MediaMetadata) {
            val bookId = fromExtras(mediaMetadata.extras, BUNDLE_KEY_BOOK_ID) ?: return
            if (bookId == _state.value.currentBookId) return

            _state.value = _state.value.copy(currentBookId = bookId)
        }
    }
}
