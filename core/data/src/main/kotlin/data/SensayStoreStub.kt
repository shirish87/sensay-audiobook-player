package data

import android.net.Uri
import data.entity.*
import data.util.ContentDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SensayStoreStub : DataStore {

    private val chapters = (1..50).map {
        Chapter.empty(bookId = 1)
            .copy(
                chapterId = it.toLong(),
                trackId = it,
                title = "Chapter $it",
                duration = ContentDuration(1.hours),
            )
    }

    private val booksProgressLibrary = mutableListOf(
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 1,
                title = "Harry Potter and the Philosopher's Stone, Book 1",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(8.hours + 25.minutes),
            ),
            chapter = chapters.first().copy(bookId = 1, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51DoG9xDIKL._SL500_.jpg")),
            chapters = chapters.take(17).map { it.copy(bookId = 1) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 1,
                chapterId = 1,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 17,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 2,
                title = "Harry Potter and the Chamber of Secrets, Book 2",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(9.hours + 43.minutes),
            ),
            chapter = chapters.first().copy(bookId = 2, coverUri = Uri.parse("https://m.media-amazon.com/images/I/61leXopTd0L._SL500_.jpg")),
            chapters = chapters.take(18).map { it.copy(bookId = 2) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 2,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 18,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 3,
                title = "Harry Potter and the Prisoner of Azkaban, Book 3",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(12.hours + 3.minutes),
            ),
            chapter = chapters.first().copy(bookId = 3, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51foVEpVklL._SL500_.jpg")),
            chapters = chapters.take(22).map { it.copy(bookId = 3, ) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 3,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 22,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 4,
                title = "Harry Potter and the Goblet of Fire, Book 4",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(20.hours + 54.minutes),
            ),
            chapter = chapters.first().copy(bookId = 4, coverUri = Uri.parse("https://m.media-amazon.com/images/I/61NP3Oa+TSL._SL500_.jpg")),
            chapters = chapters.take(37).map { it.copy(bookId = 4) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 4,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 37,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 5,
                title = "Harry Potter and the Order of the Phoenix, Book 5",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(29.hours + 1.minutes),
            ),
            chapter = chapters.first().copy(bookId = 5, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51nMvNtkYlL._SL500_.jpg")),
            chapters = chapters.take(38).map { it.copy(bookId = 5) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 5,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 38,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 6,
                title = "Harry Potter and the Half-Blood Prince, Book 6",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(20.hours + 31.minutes),
            ),
            chapter = chapters.first().copy(bookId = 6, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51AXwEQAnHL._SL500_.jpg")),
            chapters = chapters.take(30).map { it.copy(bookId = 6) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 6,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 30,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 7,
                title = "Harry Potter and the Deathly Hallows, Book 7",
                author = "J.K. Rowling",
                series = "Harry Potter",
                duration = ContentDuration(23.hours + 59.minutes),
            ),
            chapter = chapters.first().copy(bookId = 7, coverUri = Uri.parse("https://m.media-amazon.com/images/I/617QOc73e4L._SL500_.jpg")),
            chapters = chapters.take(37).map { it.copy(bookId = 7) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 7,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 37,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 8,
                title = "Athena's Champion",
                author = "David Hair, Cath Mayo",
                series = "The Olympus Series",
                duration = ContentDuration(14.hours + 48.minutes),
            ),
            chapter = chapters.first().copy(bookId = 8, coverUri = Uri.parse("https://m.media-amazon.com/images/I/61gs-58lJtL._SL500_.jpg")),
            chapters = chapters.take(31).map { it.copy(bookId = 8) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 8,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 31,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 9,
                title = "Oracle's War",
                author = "David Hair, Cath Mayo",
                series = "The Olympus Series",
                duration = ContentDuration(14.hours + 26.minutes),
            ),
            chapter = chapters.first().copy(bookId = 9, coverUri = Uri.parse("https://m.media-amazon.com/images/I/61krfBVlHvL._SL500_.jpg")),
            chapters = chapters.take(34).map { it.copy(bookId = 9) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 9,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 34,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 10,
                title = "HR: The Complete Series",
                author = "BBC Radio",
                duration = ContentDuration(13.hours + 52.minutes),
            ),
            chapter = chapters.first().copy(bookId = 10, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51Ed-Bd7lsL._SL500_.jpg")),
            chapters = chapters.take(42).map { it.copy(bookId = 10) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 10,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 42,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 11,
                title = "The Lies of Locke Lamora",
                author = "Scott Lynch",
                series = "Gentleman Bastard",
                duration = ContentDuration(22.hours + 36.minutes),
            ),
            chapter = chapters.first().copy(bookId = 11, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51q+A0B7N9L._SL500_.jpg")),
            chapters = chapters.take(16).map { it.copy(bookId = 11) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 11,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 16,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 12,
                title = "Red Seas Under Red Skies",
                author = "Scott Lynch",
                series = "Gentleman Bastard",
                duration = ContentDuration(25.hours + 56.minutes),
            ),
            chapter = chapters.first().copy(bookId = 12, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51BN5izByyL._SL250_FMwebp_.jpg")),
            chapters = chapters.take(21).map { it.copy(bookId = 12) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 12,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 21,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 13,
                title = "King of the World: The Life of Cyrus the Great",
                author = "Matt Waters",
                duration = ContentDuration(7.hours + 12.minutes),
            ),
            chapter = chapters.first().copy(bookId = 13, coverUri = Uri.parse("https://m.media-amazon.com/images/I/617qOjn+XAL._SL500_.jpg")),
            chapters = chapters.take(16).map { it.copy(bookId = 13) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 13,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 16,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 14,
                title = "Plain Tales from the Hills",
                author = "Rudyard Kipling",
                duration = ContentDuration(7.hours + 27.minutes),
            ),
            chapter = chapters.first().copy(bookId = 14, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51yFYy-kIcL._SL500_.jpg")),
            chapters = chapters.take(8).map { it.copy(bookId = 14) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 14,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 8,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 15,
                title = "The Witch's Heart",
                author = "Genevieve Gornichec",
                duration = ContentDuration(12.hours + 4.minutes),
            ),
            chapter = chapters.first().copy(bookId = 15, coverUri = Uri.parse("https://m.media-amazon.com/images/I/61YoGTFzvBL._SL250_FMwebp_.jpg")),
            chapters = chapters.take(35).map { it.copy(bookId = 15) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 15,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 35,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 16,
                title = "The Hitchhiker's Guide to the Galaxy",
                author = "Douglas Adams",
                duration = ContentDuration(5.hours + 51.minutes),
            ),
            chapter = chapters.first().copy(bookId = 16, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51b09NRSkiL._SL250_FMwebp_.jpg")),
            chapters = chapters.take(23).map { it.copy(bookId = 16) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 16,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 23,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
        BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                bookId = 17,
                title = "Strange Days",
                author = "Rick Gualtieri",
                series = "Bill of the Dead",
                duration = ContentDuration(12.hours + 4.minutes),
            ),
            chapter = chapters.first().copy(bookId = 17, coverUri = Uri.parse("https://m.media-amazon.com/images/I/51-wDABGKiS._SL250_FMwebp_.jpg")),
            chapters = chapters.take(8).map { it.copy(bookId = 17) },
            bookProgress = BookProgress.empty().copy(
                bookProgressId = 17,
                bookCategory = BookCategory.NOT_STARTED,
                chapterTitle = chapters.first().title,
                currentChapter = 0,
                totalChapters = 8,
                bookProgress = ContentDuration(0.hours),
            ),
        ),
    )

    private val booksProgressCurrent = mutableListOf<BookProgressWithBookAndChapters>()

    override fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        authorsFilter: List<String>,
        orderBy: String,
        isAscending: Boolean,
    ): Flow<List<BookProgressWithBookAndChapters>> = flowOf(
        (if (bookCategories.containsAll(listOf(BookCategory.CURRENT))) booksProgressCurrent else booksProgressLibrary)
            .filter { bookCategories.contains(it.bookProgress.bookCategory) }
            .filter { if (authorsFilter.isEmpty()) true else authorsFilter.any { author -> it.book.author?.contains(author) == true } }
            .filter {
                val f = filter.lowercase().replace("%", "")

                if (f.isBlank()) true else (
                        it.book.title.lowercase().contains(f) ||
                                it.book.author?.lowercase()?.contains(f) == true
                        )
            }
            .sortedBy {
                when (orderBy) {
                    "bookTitle" -> it.book.title
                    "bookAuthor" -> it.book.author
                    "bookSeries" -> it.book.series
                    "chapterTitle" -> it.chapter.title
                    "bookRemaining" -> it.bookProgress.bookRemaining.ms.toString()
                    "createdAt" -> it.bookProgress.createdAt.toEpochMilli().toString()
                    else -> it.bookProgress.lastUpdatedAt.toEpochMilli().toString()
                }
            }.run {
                if (!isAscending) {
                    reversed()
                } else this
            }
    ).onStart {
        delay(2000)
    }

    override fun bookAuthors(bookCategories: Collection<BookCategory>): Flow<List<String>> = flowOf(
        (if (bookCategories.containsAll(listOf(BookCategory.CURRENT))) booksProgressCurrent else booksProgressLibrary)
            .filter { bookCategories.contains(it.bookProgress.bookCategory) }
            .mapNotNull { it.book.author }
            .toSet()
            .toList()
    )

    override fun progressRestorableCount(): Flow<Int> = emptyFlow()

    override suspend fun updateBookCategory(
        @Suppress("UNUSED_PARAMETER") book: Book,
        @Suppress("UNUSED_PARAMETER") chaptersList: List<Chapter>,
        @Suppress("UNUSED_PARAMETER") bookProgress: BookProgress,
        @Suppress("UNUSED_PARAMETER") bookCategory: BookCategory,
    ): Int {

        val (src, dest) = if (bookProgress.bookCategory == BookCategory.CURRENT && bookCategory != BookCategory.CURRENT) {
            // current => library
            booksProgressCurrent to booksProgressLibrary
        } else {
            // library => current
            booksProgressLibrary to booksProgressCurrent
        }

        src.find { it.bookProgress.bookProgressId == bookProgress.bookProgressId }
            ?.apply {
                dest.add(
                    copy(bookProgress = bookProgress.copy(bookCategory = bookCategory))
                )

                src.remove(this)
            }

        return 0
    }

    override suspend fun updateBookProgress(
        @Suppress("UNUSED_PARAMETER") bookProgressVisibility: BookProgressVisibility,
    ): Int = 0

    override fun bookWithChapters(bookId: BookId): Flow<BookWithChapters> {
        val (_, book, _, chapters) = booksProgressLibrary.find {
            it.book.bookId == bookId
        } ?: booksProgressCurrent.find {
            it.book.bookId == bookId
        } ?: throw Error("Not found")

        return flowOf(BookWithChapters(book, chapters))
    }

    override fun bookProgress(bookId: BookId): Flow<BookProgress> {
        val (bookProgress) = booksProgressLibrary.find {
            it.book.bookId == bookId
        } ?: booksProgressCurrent.find {
            it.book.bookId == bookId
        } ?: throw Error("Not found")

        return flowOf(bookProgress)
    }

    override suspend fun createBookmark(bookmark: Bookmark): Long = 0L

    override suspend fun deleteBookmark(bookmark: Bookmark): Int = 0

    override suspend fun updateBookConfig(bookConfig: BookConfig): Int = 0
}
