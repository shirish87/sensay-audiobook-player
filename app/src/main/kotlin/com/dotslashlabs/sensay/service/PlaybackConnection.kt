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

class PlaybackConnection constructor(private val context: Context) {

    companion object {
        const val BUNDLE_KEY_BOOK_ID = "bookId"

        fun bookIdFromExtras(extras: Bundle?): Long? {
            if (extras == null) return null

            val bookId = extras.getLong(BUNDLE_KEY_BOOK_ID, Long.MIN_VALUE)
            return if (bookId == Long.MIN_VALUE) null else bookId
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

    private val _isConnected = MutableStateFlow(_mediaController?.isConnected == true)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isPlaying = MutableStateFlow(_mediaController?.isPlaying == true)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentBookId = MutableStateFlow<Long?>(null)
    val currentBookId: StateFlow<Long?> = _currentBookId

    fun start() {
        val controllerFuture = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()

        Futures.addCallback(controllerFuture, object : FutureCallback<MediaController> {
            override fun onSuccess(result: MediaController?) {
                _mediaController = result
                _mediaController?.addListener(playerListener)
                _isConnected.value = (result?.isConnected == true)
            }

            override fun onFailure(t: Throwable) {
                release()
            }
        }, context.mainExecutor)
    }

    fun stop() = release()

    private fun release() {
        _isConnected.value = false
        _mediaController?.removeListener(playerListener)
        _mediaController = null
    }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            player?.mediaMetadata?.let { register(it) }
        }

        private fun register(mediaMetadata: MediaMetadata) {
            val bookId = bookIdFromExtras(mediaMetadata.extras) ?: return
            if (bookId == _currentBookId.value) return

            _currentBookId.value = bookId
        }
    }
}
