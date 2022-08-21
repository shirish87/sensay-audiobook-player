package com.dotslashlabs.sensay.module

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import androidx.media3.session.MediaSession
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dotslashlabs.sensay.MainActivity
import com.dotslashlabs.sensay.service.PlaybackUpdater
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import data.SensayStore

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
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    @Provides
    @ServiceScoped
    fun providePlaybackUpdater(
        store: SensayStore,
        configStore: ConfigStore,
    ): PlaybackUpdater = PlaybackUpdater(store, configStore)

    @Provides
    @ServiceScoped
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: Player,
        playbackUpdater: PlaybackUpdater,
    ): MediaSession {
        playbackUpdater.configure(player)

        val sessionActivityPendingIntent = TaskStackBuilder.create(context).run {
            addNextIntent(Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
            })
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return MediaSession.Builder(context, player)
            .setCallback(playbackUpdater)
            .apply {
                if (sessionActivityPendingIntent != null) {
                    setSessionActivity(sessionActivityPendingIntent)
                }
            }
            .build()
    }
}
