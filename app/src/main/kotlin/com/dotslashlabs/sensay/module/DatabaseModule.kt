package com.dotslashlabs.sensay.module

import android.content.Context
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import data.SensayDatabase
import data.SensayStore
import data.repository.*
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Singleton
    @Provides
    fun provideConfigStore(@ApplicationContext context: Context) = ConfigStore.instance(context)

    @Singleton
    @Provides
    fun provideSensayDatabase(@ApplicationContext context: Context) =
        SensayDatabase.instance(context)

    @Provides
    fun provideBookDao(database: SensayDatabase) = database.bookDao()

    @Provides
    fun provideChapterDao(database: SensayDatabase) = database.chapterDao()

    @Provides
    fun provideShelfDao(database: SensayDatabase) = database.shelfDao()

    @Provides
    fun provideBookProgressDao(database: SensayDatabase) = database.bookProgressDao()

    @Provides
    fun provideSourceDao(database: SensayDatabase) = database.sourceDao()

    @Provides
    fun provideBookmarkDao(database: SensayDatabase) = database.bookmarkDao()

    @Provides
    fun provideProgressDao(database: SensayDatabase) = database.progressDao()

    @Provides
    fun bookConfigDao(database: SensayDatabase) = database.bookConfigDao()

    @Singleton
    @Provides
    fun provideSensayStore(
        bookRepository: BookRepository,
        chapterRepository: ChapterRepository,
        shelfRepository: ShelfRepository,
        bookProgressRepository: BookProgressRepository,
        sourceRepository: SourceRepository,
        bookmarkRepository: BookmarkRepository,
        progressRepository: ProgressRepository,
        bookConfigRepository: BookConfigRepository,
    ) = SensayStore(
        bookRepository,
        chapterRepository,
        shelfRepository,
        bookProgressRepository,
        sourceRepository,
        bookmarkRepository,
        progressRepository,
        bookConfigRepository,
    )
}
