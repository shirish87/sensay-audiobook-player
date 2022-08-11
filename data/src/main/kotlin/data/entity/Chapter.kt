package data.entity

import android.net.Uri
import androidx.room.*
import data.util.Time

@Entity(
    indices = [
        Index(value = ["uri", "hash"], unique = true),
    ],
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val chapterId: Long = 0,
    val uri: Uri,
    val hash: String,
    val trackId: Int,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val coverUri: Uri? = null,

    val duration: Time,
    val start: Time? = null,
    val end: Time? = null,

    val createdAt: Time = Time.now(),
)

@Entity(
    primaryKeys = ["bookId", "chapterId"],
    indices = [
        Index(value = ["chapterId"], unique = true),
    ],
)
data class BookChapterCrossRef(
    val bookId: Long,
    val chapterId: Long,

    val createdAt: Time = Time.now(),
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
