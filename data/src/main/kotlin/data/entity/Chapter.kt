package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.*
import data.util.ContentDuration
import data.util.ContentDurationOptParceler
import data.util.ContentDurationParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

typealias ChapterId = Long

@Entity(
    indices = [
        Index(value = ["uri", "hash"], unique = true),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
@TypeParceler<ContentDuration?, ContentDurationOptParceler>()
data class Chapter(
    @PrimaryKey(autoGenerate = true) val chapterId: ChapterId = 0,
    val uri: Uri,
    val hash: String,
    val trackId: Int,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val compilation: String? = null,
    val coverUri: Uri? = null,
    val lastModified: Instant? = null,

    val duration: ContentDuration,
    val start: ContentDuration,
    val end: ContentDuration,

    val createdAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = Chapter(
            uri = Uri.EMPTY,
            hash = "",
            trackId = 0,
            title = "",
            duration = ContentDuration.ZERO,
            start = ContentDuration.ZERO,
            end = ContentDuration.ZERO,
        )

        fun defaultChapter(
            book: Book,
            title: String = "Chapter 1",
            trackId: Int = 0,
        ) = Chapter(
            uri = book.uri,
            coverUri = book.coverUri,
            hash = book.hash,
            title = title,
            trackId = trackId,
            start = ContentDuration.ZERO,
            end = book.duration,
            duration = book.duration,
            lastModified = book.lastModified,
        )
    }

    fun isInvalid() = (end == ContentDuration.ZERO || start > end)
}

@Entity(
    primaryKeys = ["bookId", "chapterId"],
    indices = [
        Index(value = ["chapterId"], unique = true),
    ],
)
data class BookChapterCrossRef(
    val bookId: BookId,
    val chapterId: ChapterId,

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
