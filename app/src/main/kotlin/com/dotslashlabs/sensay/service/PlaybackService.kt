package com.dotslashlabs.sensay.service

import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import com.dotslashlabs.sensay.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import logcat.logcat
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var dataSourceFactory: DataSource.Factory

    @Inject
    lateinit var player: ExoPlayer

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent = TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@PlaybackService, MainActivity::class.java))
            getPendingIntent(0, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
        } ?: return

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {

                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    logcat { "onAddMediaItems: ${mediaItems.first().mediaMetadata.title}" }

                    return Futures.immediateFuture(
                        mediaItems.map { mediaItem ->
                            mediaItem.buildUpon()
                                .setUri(mediaItem.requestMetadata.mediaUri)
                                .build()
                        }.toMutableList()
                    )
                }
            })
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()

        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession {
        return mediaSession
    }
}
