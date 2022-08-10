package data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        BookChapterCrossRef::class,
        BookShelfCrossRef::class,
        BookTagCrossRef::class,
    ],
    version = SensayDatabase.VERSION,
    exportSchema = false,
)
@TypeConverters(DataTypeConverters::class)
abstract class SensayDatabase : RoomDatabase() {

    companion object {
        const val VERSION = 1

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
}
