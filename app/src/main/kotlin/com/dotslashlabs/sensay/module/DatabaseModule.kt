package com.dotslashlabs.sensay.module

import android.content.Context
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideConfigStore(@ApplicationContext context: Context) = ConfigStore.instance(context)
}
