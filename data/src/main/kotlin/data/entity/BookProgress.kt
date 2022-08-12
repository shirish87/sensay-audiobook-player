package data.entity

import androidx.room.*
import data.BookCategory
import data.util.Time


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
    val bookProgress: Time = Time.zero(),
    val chapterProgress: Time = Time.zero(),
    val bookCategory: BookCategory = BookCategory.NOT_STARTED,
) {

    companion object {
        fun empty() = BookProgress(
            bookId = 0L,
            chapterId = 0L,
            totalChapters = 0,
        )
    }
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
}
