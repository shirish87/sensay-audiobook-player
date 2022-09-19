package data

import android.net.Uri
import androidx.room.Transaction
import data.entity.*
import data.repository.*
import kotlinx.coroutines.flow.firstOrNull
import logcat.logcat
import java.time.Instant
import javax.inject.Inject

data class BookWithChaptersAndTags(
    val booksWithChapters: BookWithChapters,
    val tags: Collection<String>,
)

class SensayStore @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val shelfRepository: ShelfRepository,
    private val tagRepository: TagRepository,
    private val bookProgressRepository: BookProgressRepository,
    private val sourceRepository: SourceRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val progressRepository: ProgressRepository,
) {

    suspend fun createBooksWithChapters(
        sourceId: SourceId,
        booksWithChaptersAndTags: Collection<BookWithChaptersAndTags>,
    ): List<Long> {

        return booksWithChaptersAndTags.mapNotNull {
            val book = it.booksWithChapters.book
            val chapters = it.booksWithChapters.chapters.sortedBy { o -> o.trackId }
            val tags = it.tags

            if (chapters.isEmpty() || chapters.any { c -> c.isInvalid() }) {
                logcat {
                    "Invalid chapters in book '${book.title}': ${
                        chapters.joinToString(" ") { c ->
                            "(${c.trackId}) ${c.title} [${c.start.format()} => ${c.end.format()}]"
                        }
                    }"
                }

                return@mapNotNull null
            }

            var bookId: Long = -1L

            try {
                runInTransaction {
                    bookId = bookRepository.createBook(book)
                    if (bookId == -1L) return@mapNotNull null

                    val chapterIds = chapterRepository.createChapters(chapters)

                    if (chapterIds.any { c -> c == -1L }) {
                        return@mapNotNull null
                    }

                    val existingTags =
                        tagRepository.tagsByNames(tags).firstOrNull() ?: emptyList()

                    val tagIds = existingTags.map { t -> t.tagId }.plus(
                        tagRepository.createTags(
                            tags.filterNot { t -> existingTags.any { et -> et.name == t } }
                                .map { t -> Tag(name = t) },
                        )
                    )

                    tagRepository.insertBookTagCrossRefs(
                        tagIds.map { tagId ->
                            BookTagCrossRef(
                                bookId,
                                tagId,
                            )
                        }
                    )

                    chapterRepository.insertBookChapterCrossRefs(
                        chapterIds.map { chapterId ->
                            BookChapterCrossRef(
                                bookId,
                                chapterId,
                            )
                        }
                    )

                    bookProgressRepository.insertBookProgress(
                        BookProgress(
                            bookId = bookId,
                            chapterId = chapterIds.first(),
                            chapterTitle = chapters.first().title,
                            totalChapters = chapterIds.size,
                            bookTitle = book.title,
                            bookAuthor = book.author,
                            bookSeries = book.series,
                            createdAt = Instant.now(),
                            bookRemaining = book.duration,
                        )
                    )

                    sourceRepository.insertSourceBookCrossRef(
                        SourceBookCrossRef(
                            sourceId = sourceId,
                            bookId = bookId,
                        )
                    )

                    bookId
                }
            } catch (ex: Exception) {
                logcat { "createBooksWithChapters: Error importing book: ${ex.message}" }

                // cleanup
                if (bookId != -1L) {
                    bookById(bookId).firstOrNull()?.let { b ->
                        deleteBooks(listOf(b))
                    }
                }

                return@mapNotNull null
            }
        }
    }

    fun booksProgressWithBookAndChapters(bookCategories: Collection<BookCategory>) =
        bookProgressRepository.booksProgressWithBookAndChapters(bookCategories)

    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        authorsFilter: List<String>,
        orderBy: String,
        isAscending: Boolean,
    ) = bookProgressRepository.booksProgressWithBookAndChapters(
        bookCategories,
        filter,
        authorsFilter,
        orderBy,
        isAscending,
    )

    fun booksProgressWithBookAndChapters() =
        bookProgressRepository.booksProgressWithBookAndChapters()

    fun bookProgressWithBookAndChapters(bookId: BookId) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookId)

    fun bookProgressWithBookAndChapters(bookIds: Collection<BookId>) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookIds)

    fun progressRestorableCount() =
        progressRepository.progressCount()

    fun progressRestorable() =
        progressRepository.progressRestorable()

    fun booksCount() = bookRepository.booksCount()

    fun bookByUri(uri: Uri) = bookRepository.bookByUri(uri)

    fun bookById(bookId: BookId) = bookRepository.bookById(bookId)
    fun bookWithChapters(bookId: BookId) = chapterRepository.bookWithChapters(bookId)
    fun bookProgress(bookId: BookId) = bookProgressRepository.bookProgress(bookId)

    fun chaptersByUri(uri: Uri) = chapterRepository.chaptersByUri(uri)

    fun sourceById(sourceId: SourceId) = sourceRepository.sourceById(sourceId)
    fun sources() = sourceRepository.sources()
    fun sources(isActive: Boolean = true) = sourceRepository.sources(isActive)

    suspend fun addSources(sources: Collection<Source>) = sourceRepository.addSources(sources)

    suspend fun deleteSource(sourceId: SourceId): List<BookId> {
        return try {
            runInTransaction {
                val sourceWithBooks =
                    sourceRepository.sourceWithBooks(sourceId).firstOrNull() ?: return emptyList()

                if (deleteBooks(sourceWithBooks.books)) {
                    sourceRepository.deleteSource(sourceWithBooks.source)
                }

                sourceWithBooks.books.map { it.bookId }
            }
        } catch (ex: Exception) {
            emptyList()
        }
    }

    private suspend fun deleteBooks(books: Collection<Book>): Boolean {
        return try {
            runInTransaction {
                val bookIds = books.map { it.bookId }

                shelfRepository.deleteShelves(bookIds)
                tagRepository.deleteTags(bookIds)

                chapterRepository.deleteChapters(bookIds)
                bookmarkRepository.deleteBookmarksForBooks(bookIds)
                bookRepository.deleteBooks(books)

                bookProgressRepository.deleteOrResetBooksProgress(bookIds)

                true
            }
        } catch (ex: Exception) {
            false
        }
    }

    suspend fun updateBookProgress(bookProgressUpdate: BookProgressUpdate) =
        bookProgressRepository.update(bookProgressUpdate)

    suspend fun createBookmark(bookmark: Bookmark) = bookmarkRepository.createBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: Bookmark) =
        bookmarkRepository.deleteBookmarks(listOf(bookmark))

    fun bookmarksWithChapters(bookId: BookId) = bookmarkRepository.bookmarksWithChapters(bookId)

    suspend fun deleteProgress(progress: Progress) = progressRepository.deleteProgress(progress)
}

@Transaction
inline fun <reified U> SensayStore.runInTransaction(tx: () -> U): U = tx()
