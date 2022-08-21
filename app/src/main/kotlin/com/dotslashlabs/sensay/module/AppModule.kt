package com.dotslashlabs.sensay.module

import com.dotslashlabs.sensay.service.PlaybackConnection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun providePlaybackConnection() = PlaybackConnection()
}
