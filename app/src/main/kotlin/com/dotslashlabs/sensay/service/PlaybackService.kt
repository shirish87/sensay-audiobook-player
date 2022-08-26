package com.dotslashlabs.sensay.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import com.dotslashlabs.sensay.common.MediaSessionQueue
import com.dotslashlabs.sensay.common.SensayPlayer
import com.dotslashlabs.sensay.util.PlayerState
import config.ConfigStore
import dagger.hilt.android.AndroidEntryPoint
import data.SensayStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import logcat.logcat
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private val backgroundJob = Job() + CoroutineName(PlaybackService::class.java.simpleName)
    private val scope = CoroutineScope(Dispatchers.Main + backgroundJob)

    @Inject
    lateinit var player: SensayPlayer

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var store: SensayStore

    @Inject
    lateinit var configStore: ConfigStore

    @Inject
    lateinit var mediaSessionQueue: MediaSessionQueue


    override fun onCreate() {
        super.onCreate()

        scope.launch {
            if (player.isPlaying) {
                player.startServiceTracker(scope)
            }

            player.serviceEvents.collectLatest(::updateBookProgress)
        }
    }

    private suspend fun updateBookProgress(playerState: PlayerState) {
        if (player.isPlaying) {
            player.startServiceTracker(scope)
        } else {
            player.stopServiceTracker()
        }

        val mediaId = playerState.mediaId ?: return
        val chapterPosition = playerState.position ?: return
        val mediaProgress = mediaSessionQueue.getMedia(mediaId) ?: return

        val bookProgress = mediaProgress.toBookProgress(chapterPosition)
        if (bookProgress.chapterProgress > mediaProgress.chapterDuration) return
        if (bookProgress.bookProgress > mediaProgress.bookDuration) return

        withContext(Dispatchers.IO) {
            store.updateBookProgress(bookProgress)
            configStore.setLastPlayedBookId(bookProgress.bookId)
            logcat { "updated mediaId=$mediaId" }
        }
    }

    override fun onDestroy() {
        if (scope.isActive) {
            scope.cancel()
        }

        player.release()
        mediaSession.release()

        super.onDestroy()
    }

    override fun onUpdateNotification(session: MediaSession) {
        super.onUpdateNotification(session)
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession {
        return mediaSession
    }
}
