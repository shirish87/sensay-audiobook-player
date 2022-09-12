package data.entity

import android.os.Parcelable
import androidx.room.*
import data.BookCategory
import data.util.ContentDuration
import data.util.ContentDurationParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

typealias BookProgressId = Long

@Entity(
    indices = [
        Index(value = ["bookId"], unique = true),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
data class BookProgress(
    @PrimaryKey(autoGenerate = true) val bookProgressId: BookProgressId = 0,
    val bookId: BookId,
    val chapterId: ChapterId,
    val totalChapters: Int,
    val currentChapter: Int = 0,
    val bookProgress: ContentDuration = ContentDuration.ZERO,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,
    val bookCategory: BookCategory = BookCategory.NOT_STARTED,
    val lastUpdatedAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = BookProgress(
            bookId = 0L,
            chapterId = 0L,
            totalChapters = 0,
        )
    }

    fun chapterProgressDisplayFormat() = "${if (currentChapter > 0) {
        listOf(
            currentChapter,
            totalChapters,
        ).joinToString(separator = "/")
    } else totalChapters} chapters"
}

@Parcelize
data class BookProgressWithBookAndShelves(
    @Embedded
    val bookProgress: BookProgress,

    @Relation(
        parentColumn = "bookId",
        entity = Book::class,
        entityColumn = "bookId",
    )
    val book: Book,

    @Relation(
        parentColumn = "chapterId",
        entity = Chapter::class,
        entityColumn = "chapterId",
    )
    val chapter: Chapter,

    @Relation(
        parentColumn = "bookId",
        entityColumn = "shelfId",
        associateBy = Junction(BookShelfCrossRef::class),
    )
    val shelves: List<Shelf>,
) : Parcelable

@Parcelize
data class BookProgressWithBookAndChapters(
    @Embedded
    val bookProgress: BookProgress,

    @Relation(
        parentColumn = "bookId",
        entity = Book::class,
        entityColumn = "bookId",
    )
    val book: Book,

    @Relation(
        parentColumn = "chapterId",
        entity = Chapter::class,
        entityColumn = "chapterId",
    )
    val chapter: Chapter,

    @Relation(
        parentColumn = "bookId",
        entityColumn = "chapterId",
        associateBy = Junction(BookChapterCrossRef::class),
    )
    val chapters: List<Chapter>,
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    val positionMs = bookProgress.bookProgress.ms

    @IgnoredOnParcel
    @Ignore
    val durationMs = book.duration.ms

    @IgnoredOnParcel
    @Ignore
    val position = bookProgress.bookProgress.format()

    @IgnoredOnParcel
    @Ignore
    val duration = book.duration.format()

    @IgnoredOnParcel
    @Ignore
    val chapterPositionMs = bookProgress.chapterProgress.ms

    @IgnoredOnParcel
    @Ignore
    val chapterDurationMs = chapter.duration.ms

    @IgnoredOnParcel
    @Ignore
    val chapterPosition = bookProgress.chapterProgress.format()

    @IgnoredOnParcel
    @Ignore
    val chapterDuration = chapter.duration.format()

    @IgnoredOnParcel
    @Ignore
    val isEmpty = (durationMs < 1)

    companion object {
        fun empty() = BookProgressWithBookAndChapters(
            bookProgress = BookProgress.empty(),
            book = Book.empty(),
            chapter = Chapter.empty(),
            chapters = emptyList(),
        )
    }
}
