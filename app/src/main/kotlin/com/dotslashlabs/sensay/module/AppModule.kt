package com.dotslashlabs.sensay.module

import com.dotslashlabs.sensay.common.MediaSessionQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import data.SensayStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideMediaSessionQueue(sensayStore: SensayStore) = MediaSessionQueue(sensayStore)
}
