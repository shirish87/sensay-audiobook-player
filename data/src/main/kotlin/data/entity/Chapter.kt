package data.entity

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.room.*
import data.util.ContentDuration
import java.time.Instant

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

    val duration: ContentDuration,
    val start: ContentDuration? = null,
    val end: ContentDuration? = null,

    val createdAt: Instant = Instant.now(),
) {

    companion object {
        fun empty() = Chapter(
            uri = Uri.EMPTY,
            hash = "",
            trackId = 0,
            title = "",
            duration = ContentDuration.ZERO,
        )
    }

    fun toBundle() = bundleOf(
        "chapterId" to chapterId,
        "uri" to uri.toString(),
        "hash" to hash,
        "trackId" to trackId,
        "title" to title,
        "description" to description,
        "author" to author,
        "coverUri" to coverUri.toString(),
        "duration" to duration.format(),
        "start" to start?.format(),
        "end" to end?.format(),
        "createdAt" to createdAt.toEpochMilli(),
    )
}

@Entity(
    primaryKeys = ["bookId", "chapterId"],
    indices = [
        Index(value = ["chapterId"], unique = true),
    ],
)
data class BookChapterCrossRef(
    val bookId: Long,
    val chapterId: Long,

    val createdAt: Instant = Instant.now(),
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
