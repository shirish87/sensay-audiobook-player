package data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import data.DataStore
import data.SensayDatabase
import data.SensayStore
import data.SensayStoreStub
import data.repository.*
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class DataStoreImpl

@Qualifier
annotation class DataStoreStub

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) =
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
    @DataStoreImpl
    fun provideDataStore(
        bookRepository: BookRepository,
        chapterRepository: ChapterRepository,
        shelfRepository: ShelfRepository,
        bookProgressRepository: BookProgressRepository,
        sourceRepository: SourceRepository,
        bookmarkRepository: BookmarkRepository,
        progressRepository: ProgressRepository,
        bookConfigRepository: BookConfigRepository,
    ): DataStore = SensayStore(
        bookRepository,
        chapterRepository,
        shelfRepository,
        bookProgressRepository,
        sourceRepository,
        bookmarkRepository,
        progressRepository,
        bookConfigRepository,
    )

    @Singleton
    @Provides
    @DataStoreStub
    fun provideDataStoreStub(): DataStore = SensayStoreStub()
}
