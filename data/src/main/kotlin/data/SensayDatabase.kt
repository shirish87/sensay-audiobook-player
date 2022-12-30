package data

import android.content.Context
import androidx.room.*
import data.dao.*
import data.entity.*
import data.util.DataTypeConverters

@Database(
    entities = [
        Book::class,
        Chapter::class,
        Shelf::class,
        Tag::class,
        BookProgress::class,
        Source::class,
        Bookmark::class,
        BookChapterCrossRef::class,
        BookShelfCrossRef::class,
        BookTagCrossRef::class,
        SourceBookCrossRef::class,
        Progress::class,
        BookConfig::class,
    ],
    version = SensayDatabase.VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(DataTypeConverters::class)
abstract class SensayDatabase : RoomDatabase() {

    companion object {
        const val VERSION = 2

        fun instance(appContext: Context) = Room.databaseBuilder(
            appContext,
            SensayDatabase::class.java,
            "app.db",
        ).build()
    }

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun shelfDao(): ShelfDao
    abstract fun tagDao(): TagDao
    abstract fun bookProgressDao(): BookProgressDao
    abstract fun sourceDao(): SourceDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookConfigDao(): BookConfigDao
}
