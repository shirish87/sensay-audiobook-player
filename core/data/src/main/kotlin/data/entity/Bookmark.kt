package data.entity

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

typealias BookmarkId = Long

enum class BookmarkType {
    SYSTEM,
    USER,
}

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = arrayOf("bookId"),
            childColumns = arrayOf("bookId"),
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Chapter::class,
            parentColumns = arrayOf("chapterId"),
            childColumns = arrayOf("chapterId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["chapterId", "chapterPosition"], unique = true),
        Index(value = ["bookId", "chapterId"], unique = false),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
@TypeParceler<ContentDuration?, ContentDurationOptParceler>()
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val bookmarkId: BookmarkId = 0,
    val chapterId: ChapterId,
    val bookId: BookId,
    val chapterPosition: ContentDuration,
    val chapterDuration: ContentDuration,

    val title: String? = null,
    val description: String? = null,
    val bookmarkType: BookmarkType = BookmarkType.USER,

    val createdAt: Instant = Instant.now(),
) : Parcelable {

    companion object {

        fun empty(bookId: BookId, chapterId: ChapterId) = Bookmark(
            bookId = bookId,
            chapterId = chapterId,
            bookmarkType = BookmarkType.SYSTEM,
            chapterPosition = ContentDuration.ZERO,
            chapterDuration = ContentDuration.ZERO,
        )
    }

    fun isInvalid() = (chapterDuration == ContentDuration.ZERO)
}

data class BookmarkWithChapter(
    @Embedded val bookmark: Bookmark,
    @Relation(
        parentColumn = "chapterId",
        entityColumn = "chapterId",
    )
    val chapter: Chapter,
)
