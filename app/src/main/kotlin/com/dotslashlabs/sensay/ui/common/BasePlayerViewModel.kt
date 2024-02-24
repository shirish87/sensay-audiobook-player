package com.dotslashlabs.sensay.ui.common

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.bundleOf
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.work.await
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.Success
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat
import media.AudioEffectCommands
import media.BindConnection
import media.ExtraSessionCommands
import media.MediaSessionCommands
import media.PlayableMediaItem
import media.service.MediaService
import java.io.IOException

abstract class BasePlayerViewModel<S : MavericksState>(
    initialState: S,
    private val bindConnection: BindConnection<MediaService>,
    mediaServiceComponentName: ComponentName,
    context: Context,
    private val progressUpdateInterval: Long = 1000,
) : MavericksViewModel<S>(initialState) {

    protected var player: Player? = null
    protected var service: MediaService? = null

    private var serviceJob: Job? = null
    private var playerJob: Job? = null

    private val playerListener = object : Player.Listener {

        // based on https://github.com/androidx/media/blob/5328d6464acb077a7e8cba61b8cac1973c4943d7/libraries/ui/src/main/java/androidx/media3/ui/PlayerControlView.java#L1608
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdatePlayPauseState();
            }
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateProgress();
            }
            if (events.containsAny(
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateRepeatModeButton();
            }
            if (events.containsAny(
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateShuffleButton();
            }
            if (events.containsAny(
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_SEEK_BACK_INCREMENT_CHANGED,
                    Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateNavigation();
            }
            if (events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateTimeline();
            }
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdatePlaybackSpeedList();
            }
            if (events.containsAny(
                    Player.EVENT_TRACKS_CHANGED,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
                onUpdateTrackLists();
            }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            this@BasePlayerViewModel.onIsLoadingChanged()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayProgress(isPlaying, progressUpdateInterval)
        }
    }

    init {
        bindConnection.connect(object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                disconnectService(false)

                service = bindConnection.getService()
                serviceJob = onMediaServiceConnect()

                logcat { "onServiceConnected" }

                viewModelScope.launch {
                    try {
                        logcat { "try controller connection" }

                        val mediaController = MediaController.Builder(
                            context,
                            SessionToken(context, mediaServiceComponentName),
                        ).buildAsync().await().apply {
                            addListener(playerListener)
                        }

                        mediaController.waitUntilConnected {
                            player = mediaController
                            check(player != null) { "Player is null" }
                            onMediaPlayerConnect()
                            setupPlayProgressUpdates(progressUpdateInterval)
                        }
                    } catch (e: Throwable) {
                        logcat { "Error getting player: ${e.message}" }
                        e.printStackTrace()
                        disconnectService(false)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                disconnectService()
            }

            override fun onBindingDied(name: ComponentName?) {
                disconnectService()
            }

            override fun onNullBinding(name: ComponentName?) {
                disconnectService()
            }
        })
    }

    override fun onCleared() {
        disconnectService(false)

        super.onCleared()
    }

    open fun play() = player?.play()

    fun pause() = player?.pause()

    fun seekTo(position: Long, mediaItemIndex: Int? = player?.currentMediaItemIndex) =
        if (mediaItemIndex == null)
            player?.seekTo(position)
        else
            player?.seekTo(mediaItemIndex, position)

    fun seekBack() = player?.seekBack()

    fun seekForward() = player?.seekForward()

    fun previous() = player?.seekToPrevious()

    fun next() = player?.seekToNext()

    suspend fun resolveBook(bookId: Long, chapterIndex: Int = 0): Async<PlayableMediaItem> {
        val controller = (player as? MediaController?)
            ?: return Fail(Exception("Player is not connected"))

        val f = controller.sendCustomCommand(
            MediaSessionCommands.RESOLVE_MEDIA.toCommand(
                bundleOf(
                    MediaSessionCommands.KEY_BOOK_ID to bookId,
                    MediaSessionCommands.KEY_CHAPTER_INDEX to chapterIndex,
                ),
            ),
            Bundle.EMPTY,
        )

        return withContext(Dispatchers.IO) {
            val result = f.await()

            if (result.resultCode != SessionResult.RESULT_SUCCESS) {
                logcat { "ERROR: ${result.resultCode}: ${result.extras}" }

                val errorMessage = result.extras
                    .getString(MediaSessionCommands.RESULT_ARG_ERROR)
                    ?: "Error resolving media: $bookId"

                return@withContext Fail(
                    if (result.resultCode == SessionResult.RESULT_ERROR_IO)
                        IOException(errorMessage)
                    else Exception(errorMessage)
                )
            }

            @Suppress("DEPRECATION")
            val playableMediaItem: PlayableMediaItem? = result.extras
                .getParcelable(MediaSessionCommands.RESULT_ARG_PLAYABLE_MEDIA_ITEM)

            if (playableMediaItem != null)
                Success(playableMediaItem)
            else Fail(IOException("Not found"))
        }
    }

    private fun sendCustomCommand(
        command: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult>? =
        (player as? MediaController)?.sendCustomCommand(command, args)

    fun toggleBassBoost(isEnabled: Boolean) = viewModelScope.async {
        sendCustomCommand(
            AudioEffectCommands.BASS_BOOST.toCommand(),
            bundleOf(AudioEffectCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
        )?.await()
    }

    fun toggleReverb(isEnabled: Boolean) = viewModelScope.async {
        sendCustomCommand(
            AudioEffectCommands.REVERB.toCommand(),
            bundleOf(AudioEffectCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
        )?.await()
    }

    fun toggleSkipSilence(isEnabled: Boolean) = viewModelScope.async {
        sendCustomCommand(
            ExtraSessionCommands.SKIP_SILENCE.toCommand(),
            bundleOf(ExtraSessionCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
        )?.await()
    }

    protected fun updatePlayProgress(enabled: Boolean, progressUpdateInterval: Long) {
        if (enabled) {
            setupPlayProgressUpdates(progressUpdateInterval)
        } else {
            playerJob?.cancel()
        }
    }

    protected abstract suspend fun shouldUpdateProgress(): Boolean

    protected abstract fun onMediaServiceConnect(): Job?

    protected abstract fun onMediaServiceDisconnect()

    protected abstract fun onMediaPlayerConnect()

    protected abstract fun onMediaPlayerDisconnect()

    protected open fun onUpdatePlayPauseState() {
        publishPlayerUpdate()
    }

    protected open fun onUpdateProgress() {
        publishPlayerUpdate()
    }

    protected open fun onUpdateRepeatModeButton() {

    }

    protected open fun onUpdateShuffleButton() {

    }

    protected open fun onUpdateNavigation() {
        publishPlayerUpdate()
    }

    protected open fun onUpdateTimeline() {
        publishPlayerUpdate()
    }

    protected open fun onUpdatePlaybackSpeedList() {

    }

    protected open fun onUpdateTrackLists() {
        publishFullUpdate()
    }

    protected open fun onIsLoadingChanged() {
        publishPlayerUpdate()
    }

    protected abstract fun publishPlayerUpdate()

    protected abstract fun publishFullUpdate()

    private fun setupPlayProgressUpdates(progressUpdateInterval: Long) {
        playerJob?.cancel()

        playerJob = viewModelScope.launch {
            logcat { "setupPlayProgressUpdates: ${!shouldUpdateProgress()}" }
            if (!shouldUpdateProgress()) return@launch
            logcat { "setupPlayProgressUpdates: enabled" }

            withContext(Dispatchers.IO) {
                while (isActive) {
                    withContext(Dispatchers.Main) { onUpdateProgress() }
                    delay(progressUpdateInterval)
                }
            }
        }
    }

    private fun disconnectService(notify: Boolean = true) {
        playerJob?.cancel()
        serviceJob?.cancel()

        player?.removeListener(playerListener)
        player?.release()
        player = null
        service = null

        bindConnection.disconnect()

        if (notify) {
            onMediaPlayerDisconnect()
            onMediaServiceDisconnect()
        }
    }
}

suspend fun MediaController.waitUntilConnected(
    checkInterval: Long = 50,
    maxWait: Long = 1000,
    next: () -> Unit?,
) = withContext(Dispatchers.IO) {

    var wait = maxOf(maxWait, checkInterval + 1)

    while (isActive && !isConnected && wait > 0) {
        delay(checkInterval)
        wait -= checkInterval
    }

    if (!isActive) return@withContext
    check(isConnected) { "Controller is not connected" }

    withContext(Dispatchers.Main) { next() }
}
