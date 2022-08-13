package data.entity

import androidx.core.os.bundleOf
import androidx.room.*
import data.BookCategory
import data.util.ContentDuration
import java.time.Instant


@Entity(
    indices = [
        Index(value = ["bookId"], unique = true),
    ],
)
data class BookProgress(
    @PrimaryKey(autoGenerate = true) val bookProgressId: Long = 0,
    val bookId: Long,
    val chapterId: Long,
    val totalChapters: Int,
    val currentChapter: Int = 0,
    val bookProgress: ContentDuration = ContentDuration.ZERO,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,
    val bookCategory: BookCategory = BookCategory.NOT_STARTED,
    val lastUpdatedAt: Instant = Instant.now(),
) {

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
        ).joinToString(separator = " / ")
    } else totalChapters} chapters"

    fun toBundle() = bundleOf(
        "bookId" to bookId,
        "chapterId" to chapterId,
        "totalChapters" to totalChapters,
        "currentChapter" to currentChapter,
        "bookProgress" to bookProgress.format(),
        "chapterProgress" to chapterProgress.format(),
        "bookCategory" to bookCategory.name,
        "lastUpdatedAt" to lastUpdatedAt.toEpochMilli(),
    )
}

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
)

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
) {

    companion object {
        fun empty() = BookProgressWithBookAndChapters(
            bookProgress = BookProgress.empty(),
            book = Book.empty(),
            chapter = Chapter.empty(),
            chapters = emptyList(),
        )
    }

    fun toBundle() = bundleOf(
        "bookProgress" to bookProgress.toBundle(),
        "book" to book.toBundle(),
        "chapter" to chapter.toBundle(),
    )
}
