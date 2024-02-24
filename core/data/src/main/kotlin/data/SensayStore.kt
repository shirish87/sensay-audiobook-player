package data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.Transaction
import data.entity.Book
import data.entity.BookConfig
import data.entity.BookId
import data.entity.BookProgress
import data.entity.BookSourceScan
import data.entity.BookWithChapters
import data.entity.Bookmark
import data.entity.Chapter
import data.entity.Progress
import data.entity.Source
import data.entity.SourceId
import data.repository.BookConfigRepository
import data.repository.BookProgressRepository
import data.repository.BookRepository
import data.repository.BookmarkRepository
import data.repository.ChapterRepository
import data.repository.ProgressRepository
import data.repository.ShelfRepository
import data.repository.SourceRepository
import data.util.ContentDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import logcat.logcat
import java.time.Instant
import javax.inject.Inject

data class SourceBookWithChaptersAndTags(
    val sourceId: SourceId,
    val bookWithChapters: BookWithChapters,
    val tags: Collection<String>,
)

class SensayStore @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val shelfRepository: ShelfRepository,
    private val bookProgressRepository: BookProgressRepository,
    private val sourceRepository: SourceRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val progressRepository: ProgressRepository,
    private val bookConfigRepository: BookConfigRepository,
) : DataStore {

    companion object {
        fun contentUri(bookId: Long, chapterIndex: Int) = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .appendPath("books")
            .appendPath(bookId.toString())
            .appendPath("chapters")
            .appendPath(chapterIndex.toString())
            .build()

        fun mediaId(bookId: Long, chapterIndex: Int) = contentUri(bookId, chapterIndex).toString()
    }

    suspend fun startSourceScan(sourceId: SourceId) {
        logcat { "startSourceScan: sourceId=$sourceId" }

        runInTransaction {
            sourceRepository.updateSource(
                sourceId = sourceId,
                isScanning = true,
            )

            sourceRepository.updateSourceBooks(
                sourceId = sourceId,
                isActive = false,
                inactiveReason = InactiveReason.SCANNING,
            )
        }
    }

    suspend fun endSourceScan(sourceId: SourceId) {
        logcat { "endSourceScan: sourceId=$sourceId" }

        runInTransaction {
            sourceRepository.sourceBooks(sourceId).firstOrNull()?.forEach { sourceBook ->
                bookRepository.updateBook(
                    bookId = sourceBook.bookId,
                    scanInstant = sourceBook.scanInstant,
                    isActive = sourceBook.isActive,
                    inactiveReason = if (sourceBook.isActive) null else InactiveReason.NOT_FOUND,
                )
            }

            logcat { "sourceRepository.updateSource: sourceId=$sourceId" }
            sourceRepository.updateSource(
                sourceId = sourceId,
                isScanning = false,
            )
        }
    }

    suspend fun updateSourceBook(
        sourceId: SourceId,
        bookId: BookId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
    ) = sourceRepository.updateSourceBook(sourceId, bookId, isActive, inactiveReason)

    suspend fun createOrUpdateBooksWithChapters(
        sourceId: SourceId,
        booksWithChaptersAndTags: Collection<SourceBookWithChaptersAndTags>,
        scanInstant: Instant,
    ): List<Long> {

        return booksWithChaptersAndTags.mapNotNull {
            val book = it.bookWithChapters.book
            logcat { "Attempting book: ${book.title}" }

            val chapters = it.bookWithChapters.chapters.sortedBy { o -> o.trackId }.toMutableList()

//            if (chapters.isEmpty() && !book.duration.isEmpty()) {
//                // book is not chapterized
//                // TODO: add a single "synthetic" default chapter
////                val contentUri = contentUri(book.bookId, 0)
////                chapters.add(Chapter.defaultChapter(book, contentUri, book.uri))
//            }

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
                    val existingBook = bookRepository.bookByHash(book.hash).firstOrNull()

                    if (existingBook != null) {
//                        val oldBookProgress = bookProgressRepository.bookProgressWithBookAndChapters(
//                            existingBook.bookId,
//                        )
                        bookRepository.deleteBooks(listOf(existingBook))
                    }

                    bookId = bookRepository.createBook(book)

                    if (bookId == -1L) return@mapNotNull null

                    logcat { "Found book: bookId=$bookId sourceId=$sourceId" }
                    bookConfigRepository.insertBookConfig(BookConfig(bookId))

                    val chapterIds = chapterRepository.createChapters(
                        chapters.map { c -> c.copy(bookId = bookId) }
                    )

                    if (chapterIds.isEmpty() || chapterIds.any { c -> c == -1L }) {
                        return@mapNotNull null
                    }

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

                    bookId
                }.also { processedBookId ->
                    logcat { "upsertBookSourceScan: bookId=$processedBookId sourceId=$sourceId scanInstant=$scanInstant ${book.title}" }

                    sourceRepository.upsertBookSourceScan(
                        BookSourceScan(
                            bookId = processedBookId,
                            sourceId = sourceId,
                            scanInstant = scanInstant,
                            isActive = true,
                            inactiveReason = null,
                            isRemote = false,
//                            book.uri.scheme
//                                ?.startsWith("http", ignoreCase = true) == true,
                        ),
                    )
                }
            } catch (ex: Exception) {
                logcat { "createBooksWithChapters: Error importing book: ${ex.message}" }

                // cleanup on error
                if (bookId != -1L) {
                    deleteBooks(listOf(Book.empty().copy(bookId = bookId)))
                }

                return@mapNotNull null
            }
        }
    }

    fun booksProgressWithBookAndChapters() =
        bookProgressRepository.booksProgressWithBookAndChapters()

    override fun booksProgressWithBookAndChapters(
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

    fun bookProgressWithBookAndChapters(bookId: BookId) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookId)

    fun bookProgressWithBookAndChapters(bookIds: Collection<BookId>) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookIds)

    override fun progressRestorableCount() =
        progressRepository.progressCount()

    fun progressRestorable() =
        progressRepository.progressRestorable()

    fun bookById(bookId: BookId) = bookRepository.bookById(bookId)

    override fun bookAuthors(bookCategories: Collection<BookCategory>) =
        bookProgressRepository.bookAuthors(bookCategories)

    fun bookSeries(bookCategories: Collection<BookCategory>) =
        bookProgressRepository.bookSeries(bookCategories)

    override fun bookWithChapters(bookId: BookId) = chapterRepository.bookWithChapters(bookId)
    override fun bookProgress(bookId: BookId) = bookProgressRepository.bookProgress(bookId)

    fun chaptersByUri(uri: Uri) = chapterRepository.chaptersByUri(uri)

    fun chaptersMaxLastModifiedByUri(uri: Uri) = chapterRepository.chaptersMaxLastModifiedByUri(uri)

    fun sourceById(sourceId: SourceId) = sourceRepository.sourceById(sourceId)
    fun sources() = sourceRepository.sources()
    fun sources(isActive: Boolean = true) = sourceRepository.sources(isActive)

    fun bookSourceScansWithBooks(sourceId: SourceId) = sourceRepository.bookSourceScansWithBooks(sourceId)

    suspend fun addSources(sources: Collection<Source>) = sourceRepository.addSources(sources)

    suspend fun deleteSource(sourceId: SourceId): List<BookId> {
        return try {
            runInTransaction {
                val sourceBooks =
                    sourceRepository.bookSourceScansWithBooks(sourceId).firstOrNull() ?: return emptyList()
                val books = sourceBooks.map { it.book }

                deleteBooks(books)
                sourceRepository.deleteSource(sourceId)
                books.map { it.bookId }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            emptyList()
        }
    }

    private suspend fun deleteBooks(books: Collection<Book>, batchSize: Int = 25): Boolean {
        return try {
            runInTransaction {
                books.chunked(batchSize).sumOf { bookRepository.deleteBooks(it) } > 0
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    override suspend fun updateBookCategory(
        book: Book,
        chaptersList: List<Chapter>,
        bookProgress: BookProgress,
        bookCategory: BookCategory,
    ): Int {

        val chapters = chaptersList.sortedBy { it.trackId }

        val chapter = chapters.run {
            if (bookCategory == BookCategory.FINISHED) {
                last()
            } else {
                first()
            }
        }

        val bookProgressContentDuration = if (bookCategory == BookCategory.FINISHED)
            book.duration
        else chapter.start

        val update = BookProgressUpdate(
            bookProgressId = bookProgress.bookProgressId,
            bookCategory = bookCategory,
            chapterId = chapter.chapterId,
            currentChapter = if (bookCategory == BookCategory.FINISHED)
                bookProgress.totalChapters else 0,
            chapterProgress = if (bookCategory == BookCategory.FINISHED)
                chapter.duration else ContentDuration.ZERO,
            chapterTitle = chapter.title,
            bookProgress = bookProgressContentDuration,
            bookRemaining = ContentDuration.ms(
                book.duration.ms - bookProgressContentDuration.ms
            ),
        )

        return updateBookProgress(update)
    }

    suspend fun updateBookProgress(bookProgressUpdate: BookProgressUpdate) =
        bookProgressRepository.update(bookProgressUpdate)

    override suspend fun updateBookProgress(bookProgressVisibility: BookProgressVisibility) =
        bookProgressRepository.update(bookProgressVisibility)

    override suspend fun createBookmark(bookmark: Bookmark) = bookmarkRepository.createBookmark(bookmark)

    override suspend fun deleteBookmark(bookmark: Bookmark) =
        bookmarkRepository.deleteBookmarks(listOf(bookmark))

    fun bookmarksWithChapters(bookId: BookId) = bookmarkRepository.bookmarksWithChapters(bookId)

    suspend fun deleteProgress(progress: Progress) = progressRepository.deleteProgress(progress)

    suspend fun ensureBookConfig(bookId: BookId) {
        val bookConfig = bookConfigRepository.bookConfig(bookId).firstOrNull()
        if (bookConfig == null) {
            bookConfigRepository.insertBookConfig(BookConfig(bookId = bookId))
        }
    }

    fun bookConfig(bookId: BookId): Flow<BookConfig> = bookConfigRepository.bookConfig(bookId)

    override suspend fun updateBookConfig(bookConfig: BookConfig) =
        bookConfigRepository.updateBookConfig(bookConfig)
}

@Transaction
inline fun <reified U> SensayStore.runInTransaction(tx: () -> U): U = tx()
