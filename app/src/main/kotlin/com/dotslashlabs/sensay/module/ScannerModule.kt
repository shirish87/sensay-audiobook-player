package com.dotslashlabs.sensay.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import scanner.CoverScanner
import scanner.MediaAnalyzer
import scanner.MediaScanner

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
