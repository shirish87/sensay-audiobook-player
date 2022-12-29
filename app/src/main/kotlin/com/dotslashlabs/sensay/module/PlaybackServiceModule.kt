package com.dotslashlabs.sensay.module

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.dotslashlabs.sensay.MainActivity
import com.dotslashlabs.sensay.common.MediaSessionQueue
import com.dotslashlabs.sensay.common.SensayPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@UnstableApi
@InstallIn(ServiceComponent::class)
@Module
object PlaybackServiceModule {

    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Provides
    @ServiceScoped
    fun providePlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
    ): Player = ExoPlayer.Builder(context)
        .setRenderersFactory(DefaultRenderersFactory(context).setEnableAudioOffload(true))
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    @Provides
    @ServiceScoped
    fun provideSensayPlayer(
        player: Player,
    ): SensayPlayer = SensayPlayer(player)

    @Provides
    @ServiceScoped
    fun provideMediaSession(
        @ApplicationContext context: Context,
        sensayPlayer: SensayPlayer,
        mediaSessionQueue: MediaSessionQueue,
    ): MediaSession {

        val resultIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
        }

        val sessionActivityPendingIntent = TaskStackBuilder.create(context)
            .addNextIntent(resultIntent)
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return MediaSession.Builder(context, sensayPlayer.player)
            .setCallback(mediaSessionQueue.mediaSessionCallback)
            .apply {
                if (sessionActivityPendingIntent != null) {
                    setSessionActivity(sessionActivityPendingIntent)
                }
            }
            .build()
    }
}
