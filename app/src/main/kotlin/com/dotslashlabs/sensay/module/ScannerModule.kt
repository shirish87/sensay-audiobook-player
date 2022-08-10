package com.dotslashlabs.sensay.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideMediaAnalyzer(@ApplicationContext context: Context) = MediaAnalyzer(context)

    @Provides
    @Singleton
    fun provideMediaScanner(mediaAnalyzer: MediaAnalyzer) = MediaScanner(mediaAnalyzer)

    @Provides
    @Singleton
    fun provideCoverScanner(@ApplicationContext context: Context) = CoverScanner(context)
}
