package com.dotslashlabs.sensay.module

import android.content.Context
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
@Module
object FakeDatabaseModule {
    @Provides
    @Singleton
    fun provideConfigStore(@ApplicationContext context: Context) = ConfigStore.instance(context)
}
