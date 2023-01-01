package com.dotslashlabs.sensay.module

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import config.ConfigStore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import data.SensayDatabase
import data.SensayStore
import data.repository.*
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
@Module
object FakeDatabaseModule {
    private const val TEST_DATASTORE_NAME: String = "test_datastore"

    @Singleton
    @Provides
    fun provideTestDispatcher() = StandardTestDispatcher()

//    @Singleton
    @Provides
    fun provideConfigStore(
        @ApplicationContext context: Context,
        dispatcher: TestDispatcher,
    ) = ConfigStore(
        PreferenceDataStoreFactory.create(
            scope = TestScope(dispatcher + Job()),
            produceFile = { context.preferencesDataStoreFile(TEST_DATASTORE_NAME) },
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() },
            ),
            migrations = listOf(
                SharedPreferencesMigration(context, TEST_DATASTORE_NAME),
            ),
        ),
    )

    @Singleton
    @Provides
    fun provideSensayDatabase(@ApplicationContext context: Context): SensayDatabase {
        return Room.inMemoryDatabaseBuilder(context, SensayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

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
        tagRepository: TagRepository,
        bookProgressRepository: BookProgressRepository,
        sourceRepository: SourceRepository,
        bookmarkRepository: BookmarkRepository,
        progressRepository: ProgressRepository,
        bookConfigRepository: BookConfigRepository,
    ) = SensayStore(
        bookRepository,
        chapterRepository,
        shelfRepository,
        tagRepository,
        bookProgressRepository,
        sourceRepository,
        bookmarkRepository,
        progressRepository,
        bookConfigRepository,
    )
}
