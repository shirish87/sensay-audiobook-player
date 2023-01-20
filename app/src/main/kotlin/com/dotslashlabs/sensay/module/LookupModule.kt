package com.dotslashlabs.sensay.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import lookup.BookLookup
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object LookupModule {

    @Provides
    @Singleton
    fun provideBookLookup() = BookLookup()
}
