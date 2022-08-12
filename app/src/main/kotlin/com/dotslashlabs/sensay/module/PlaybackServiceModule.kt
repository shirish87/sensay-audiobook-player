package com.dotslashlabs.sensay.module

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import java.io.File

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
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
    ): ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
        }

    @Provides
    @ServiceScoped
    fun provideDefaultDataSourceFactory(
        @ApplicationContext context: Context,
    ) = DefaultDataSource.Factory(context)

    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        datasourceFactory: DefaultDataSource.Factory,
    ): DataSource.Factory {
        val cacheDir = File(context.cacheDir, "playback-cache")
        val databaseProvider = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)

        return CacheDataSource.Factory().apply {
            setCache(cache)
            setUpstreamDataSourceFactory(datasourceFactory)
        }
    }
}
