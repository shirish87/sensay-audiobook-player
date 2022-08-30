package com.dotslashlabs.sensay.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import scanner.CoverScanner
import scanner.MediaAnalyzer
import scanner.MediaScanner
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ScannerModule {

    @Provides
    @Singleton
    fun provideMediaAnalyzer() = MediaAnalyzer()

    @Provides
    @Singleton
    fun provideMediaScanner(mediaAnalyzer: MediaAnalyzer) = MediaScanner(mediaAnalyzer)

    @Provides
    @Singleton
    fun provideCoverScanner() = CoverScanner()
}
