package data

import android.net.Uri
import androidx.room.Transaction
import data.entity.*
import data.repository.*
import kotlinx.coroutines.flow.*
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
    private val bookConfigRepository: BookConfigRepository,
) {

    suspend fun createOrUpdateBooksWithChapters(
        sourceId: SourceId,
        booksWithChaptersAndTags: Collection<BookWithChaptersAndTags>,
    ): List<Long> {

        return booksWithChaptersAndTags.mapNotNull {
            val book = it.booksWithChapters.book
            logcat { "Attempting book: ${book.title}" }

            val chapters = it.booksWithChapters.chapters.sortedBy { o -> o.trackId }.toMutableList()
            val tags = it.tags

            if (chapters.isEmpty() && !book.duration.isEmpty()) {
                // book is not chapterized
                // add a single "synthetic" default chapter
                chapters.add(Chapter.defaultChapter(book))
            }

            if (chapters.isEmpty() || chapters.any { c -> c.isInvalid() }) {
                logcat {
                    "Invalid chapters in book '${book.title}': ${
                    chapters.joinToString(" ") { c ->
                        "(${c.trackId}) ${c.title} [${c.start.format()} => ${c.end.format()}]"
                    }} (${chapters.size})"
                }

                logcat { "mapNotNull: ${book.title}" }
                return@mapNotNull null
            }

            logcat { "Creating book: ${book.title}" }
            var bookId: Long = -1L

            try {
                runInTransaction {
                    val existingBook = bookRepository.bookByUri(book.uri).firstOrNull()

                    bookId = if (existingBook != null) {
                        bookRepository.updateBook(book.copy(bookId = existingBook.bookId))
                        existingBook.bookId
                    } else
                        bookRepository.createBook(book)

                    if (bookId == -1L) return@mapNotNull null

                    if (existingBook != null) {
                        chapterRepository.deleteChapters(listOf(bookId))
                        bookProgressRepository.deleteOrResetBooksProgress(listOf(bookId))
                        // TODO: code to restore bookProgress
                    }

                    val chapterIds = chapterRepository.createChapters(chapters)
                    if (chapterIds.any { c -> c == -1L }) {
                        return@mapNotNull null
                    }

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

                    if (existingBook != null) {
                        tagRepository.deleteTags(listOf(bookId))
                    }

                    val tagIds = tagRepository.createOrGetTags(tags)
                    tagRepository.insertBookTagCrossRefs(
                        tagIds.map { tagId ->
                            BookTagCrossRef(
                                bookId,
                                tagId,
                            )
                        }
                    )

                    if (existingBook != null) {
                        sourceRepository.deleteSourceBookCrossRefByBook(bookId)
                    }

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
            ex.printStackTrace()
            emptyList()
        }
    }

    private suspend fun deleteBooks(books: Collection<Book>, batchSize: Int = 25): Boolean {
        return try {
            runInTransaction {
                books.chunked(batchSize).map { chunk ->
                    val bookIds = chunk.map { it.bookId }
                    bookProgressRepository.deleteOrResetBooksProgress(bookIds, batchSize)

                    shelfRepository.deleteShelves(bookIds)
                    tagRepository.deleteTags(bookIds)

                    chapterRepository.deleteChapters(bookIds, batchSize)
                    bookmarkRepository.deleteBookmarksForBooks(bookIds)
                    bookRepository.deleteBooks(chunk)
                }

                true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    suspend fun updateBookProgress(bookProgressUpdate: BookProgressUpdate) =
        bookProgressRepository.update(bookProgressUpdate)

    suspend fun updateBookProgress(bookProgressVisibility: BookProgressVisibility) =
        bookProgressRepository.update(bookProgressVisibility)

    suspend fun createBookmark(bookmark: Bookmark) = bookmarkRepository.createBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: Bookmark) =
        bookmarkRepository.deleteBookmarks(listOf(bookmark))

    fun bookmarksWithChapters(bookId: BookId) = bookmarkRepository.bookmarksWithChapters(bookId)

    suspend fun deleteProgress(progress: Progress) = progressRepository.deleteProgress(progress)

    fun ensureBookConfig(bookId: BookId): Flow<BookConfig> = flow {
        val bookConfig = bookConfigRepository.bookConfig(bookId).firstOrNull()
        if (bookConfig != null) {
            return@flow emit(bookConfig)
        }

        val id = bookConfigRepository.createBookConfig(BookConfig(bookId = bookId))
        emitAll(bookConfigRepository.bookConfig(id))
    }

    suspend fun updateBookConfig(bookConfig: BookConfig) =
        bookConfigRepository.updateBookConfig(bookConfig)
}

@Transaction
inline fun <reified U> SensayStore.runInTransaction(tx: () -> U): U = tx()
