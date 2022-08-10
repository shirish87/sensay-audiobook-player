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
    fun provideSensayDatabase(@ApplicationContext context: Context) = SensayDatabase.instance(context)

    @Provides
    fun provideBookDao(database: SensayDatabase) = database.bookDao()

    @Provides
    fun provideChapterDao(database: SensayDatabase) = database.chapterDao()

    @Provides
    fun provideShelfDao(database: SensayDatabase) = database.shelfDao()

    @Provides
    fun provideTagDao(database: SensayDatabase) = database.tagDao()

    @Provides
    fun provideBookProgressDao(database: SensayDatabase) = database.bookProgressDao()

    @Singleton
    @Provides
    fun provideSensayStore(
        bookRepository: BookRepository,
        chapterRepository: ChapterRepository,
        shelfRepository: ShelfRepository,
        tagRepository: TagRepository,
        bookProgressRepository: BookProgressRepository,
    ) = SensayStore(
        bookRepository,
        chapterRepository,
        shelfRepository,
        tagRepository,
        bookProgressRepository,
    )
}
