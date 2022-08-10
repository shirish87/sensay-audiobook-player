package data.entity

import android.net.Uri
import androidx.room.*
import data.util.Time

@Entity
data class Chapter(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "chapterId") val chapterId: Long = 0,
    @ColumnInfo(name = "hash", index = true) val hash: String,
    @ColumnInfo(name = "uri") val uri: Uri,
    @ColumnInfo(name = "trackId") val trackId: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "author") val author: String? = null,
    @ColumnInfo(name = "coverUri") val coverUri: Uri? = null,

    @ColumnInfo(name = "duration") val duration: Time,
    @ColumnInfo(name = "start") val start: Time? = null,
    @ColumnInfo(name = "end") val end: Time? = null,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
)

@Entity(primaryKeys = ["bookId", "chapterId"])
data class BookChapterCrossRef(
    @ColumnInfo(name = "bookId") val bookId: Long,
    @ColumnInfo(name = "chapterId", index = true) val chapterId: Long,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
)

data class BookWithChapters(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "chapterId",
        associateBy = Junction(BookChapterCrossRef::class),
    )
    val chapters: List<Chapter>,
)

data class ChapterWithBook(
    @Embedded val chapter: Chapter,
    @Relation(
        parentColumn = "chapterId",
        entityColumn = "bookId",
        associateBy = Junction(BookChapterCrossRef::class),
    )
    val book: Book,
)
