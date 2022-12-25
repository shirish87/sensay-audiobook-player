package data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import data.util.ContentDuration
import data.util.ContentDurationParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

typealias ProgressId = Long

@Entity(
    indices = [
        Index(value = ["bookTitle", "bookDuration", "totalChapters"], unique = true),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
data class Progress(
    @PrimaryKey(autoGenerate = true) val progressId: ProgressId = 0,
    val chapterTitle: String,
    val bookTitle: String,
    val bookAuthor: String?,
    val bookSeries: String?,

    val totalChapters: Int,
    val currentChapter: Int = 0,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,

    val bookDuration: ContentDuration,
    val bookProgress: ContentDuration = ContentDuration.ZERO,

    val createdAt: Instant = Instant.now(),
    val lastUpdatedAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = Progress(
            chapterTitle = "",
            bookTitle = "",
            bookAuthor = null,
            bookSeries = null,
            totalChapters = 0,
            bookDuration = ContentDuration.ZERO,
        )

        fun from(bookProgress: BookProgress) = Progress(
            chapterTitle = bookProgress.chapterTitle,
            bookTitle = bookProgress.bookTitle,
            bookAuthor = bookProgress.bookAuthor,
            bookSeries = bookProgress.bookSeries,

            totalChapters = bookProgress.totalChapters,
            currentChapter = bookProgress.currentChapter,
            chapterProgress = bookProgress.chapterProgress,

            bookDuration = bookProgress.bookDuration,
            bookProgress = bookProgress.bookProgress,

            createdAt = bookProgress.createdAt,
        )
    }

    fun chapterProgressDisplayFormat() = "${if (currentChapter > 0) {
        listOf(
            currentChapter,
            totalChapters,
        ).joinToString(separator = "/")
    } else totalChapters} chapters"

    fun toBookProgress(
        bookProgressId: BookProgressId,
        bookId: BookId,
        chapterId: ChapterId,
    ) = BookProgress(
        bookProgressId = bookProgressId,
        bookId = bookId,
        chapterId = chapterId,

        chapterTitle = chapterTitle,
        bookTitle = bookTitle,
        bookAuthor = bookAuthor,
        bookSeries = bookSeries,

        totalChapters = totalChapters,
        currentChapter = currentChapter,
        chapterProgress = chapterProgress,

        bookRemaining = ContentDuration.ms(bookDuration.ms - bookProgress.ms),
        bookProgress = bookProgress,

        createdAt = createdAt,
    )
}
