package data.entity

import androidx.room.*
import data.BookCategory
import data.util.Time


@Entity
data class BookProgress(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "bookProgressId") val bookProgressId: Long = 0,
    @ColumnInfo(name = "bookId", index = true) val bookId: Long,
    @ColumnInfo(name = "chapterId") val chapterId: Long,
    @ColumnInfo(name = "totalChapters") val totalChapters: Int,
    @ColumnInfo(name = "currentChapter") val currentChapter: Int = 0,
    @ColumnInfo(name = "bookProgress") val bookProgress: Time = Time.zero(),
    @ColumnInfo(name = "chapterProgress") val chapterProgress: Time = Time.zero(),
    @ColumnInfo(name = "bookCategory") val bookCategory: BookCategory = BookCategory.NOT_STARTED,
)

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
)
