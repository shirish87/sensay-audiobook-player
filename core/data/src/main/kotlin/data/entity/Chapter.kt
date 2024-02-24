package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import data.util.ContentDuration
import data.util.ContentDurationOptParceler
import data.util.ContentDurationParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

typealias ChapterId = Long

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = arrayOf("bookId"),
            childColumns = arrayOf("bookId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId", "trackId"], unique = true),
        Index(value = ["uri", "trackId"], unique = true),
        Index(value = ["bookId"]),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
@TypeParceler<ContentDuration?, ContentDurationOptParceler>()
data class Chapter(
    @PrimaryKey(autoGenerate = true) val chapterId: ChapterId = 0,
    val bookId: BookId,
    val uri: Uri,
    val trackId: Int,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val compilation: String? = null,
    val coverUri: Uri? = null,
    val srcCoverByteSize: Int? = 0,
    val lastModified: Instant? = null,

    val duration: ContentDuration,
    val start: ContentDuration,
    val end: ContentDuration,

    val createdAt: Instant = Instant.now(),
    val scanInstant: Instant? = null,
) : Parcelable {

    companion object {
        fun empty(bookId: BookId) = Chapter(
            bookId = bookId,
            uri = Uri.EMPTY,
            trackId = 0,
            title = "",
            duration = ContentDuration.ZERO,
            start = ContentDuration.ZERO,
            end = ContentDuration.ZERO,
        )

//        fun defaultChapter(
//            book: Book,
//            uri: Uri,
//            title: String = "Chapter 1",
//            trackId: Int = 0,
//        ) = Chapter(
//            bookId = book.bookId,
//            uri = uri,
//            title = title,
//            trackId = trackId,
//            start = ContentDuration.ZERO,
//            end = book.duration,
//            duration = book.duration,
//            lastModified = book.lastModified,
//        )
    }

    fun isInvalid() = (end == ContentDuration.ZERO || start > end)
}

data class BookWithChapters(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "bookId",
    )
    val chapters: List<Chapter>,
)
