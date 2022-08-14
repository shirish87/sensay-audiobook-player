package data

import android.net.Uri
import androidx.room.Transaction
import data.entity.*
import data.repository.*
import kotlinx.coroutines.flow.firstOrNull
import logcat.logcat
import javax.inject.Inject

class SensayStore @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val shelfRepository: ShelfRepository,
    private val tagRepository: TagRepository,
    private val bookProgressRepository: BookProgressRepository,
    private val sourceRepository: SourceRepository,
) {

    suspend fun createBooksWithChapters(
        sourceId: Long,
        booksWithChapters: List<BookWithChapters>,
    ): List<Long> {

        return booksWithChapters.mapNotNull {
            if (it.chapters.isEmpty()) {
                return@mapNotNull null
            }

            try {
                val bookId = bookRepository.createBook(it.book)
                if (bookId == -1L) return@mapNotNull null

                val chapterIds = chapterRepository.createChapters(
                    it.chapters.sortedBy { o -> o.trackId },
                )

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
                        totalChapters = chapterIds.size,
                    )
                )

                sourceRepository.insertSourceBookCrossRef(
                    SourceBookCrossRef(
                        sourceId = sourceId,
                        bookId = bookId,
                    )
                )

                bookId
            } catch (ex: Exception) {
                logcat { "createBooksWithChapters: Error importing book: ${ex.message}" }
                return@mapNotNull null
            }
        }
    }

    fun booksProgressWithBookAndChapters(bookCategories: Collection<BookCategory>) =
        bookProgressRepository.booksProgressWithBookAndChapters(bookCategories)

    fun booksProgressWithBookAndChapters() =
        bookProgressRepository.booksProgressWithBookAndChapters()

    fun bookProgressWithBookAndChapters(bookId: Long) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookId)

    fun bookProgressWithBookAndChapters(bookIds: Collection<Long>) =
        bookProgressRepository.bookProgressWithBookAndChapters(bookIds)

    fun booksCount() = bookRepository.booksCount()

    fun bookByUri(uri: Uri) = bookRepository.bookByUri(uri)

    fun sources() = sourceRepository.sources()
    fun sources(isActive: Boolean = true) = sourceRepository.sources(isActive)

    suspend fun addSources(sources: Collection<Source>) = sourceRepository.addSources(sources)

    suspend fun deleteSource(sourceId: Long): Boolean {
        try {
            runInTransaction {
                val sourceWithBooks =
                    sourceRepository.sourceWithBooks(sourceId).firstOrNull() ?: return false
                val bookIds = sourceWithBooks.books.map { it.bookId }

                shelfRepository.deleteShelves(bookIds)
                tagRepository.deleteTags(bookIds)
                bookProgressRepository.deleteBooksProgress(bookIds)
                chapterRepository.deleteChapters(bookIds)
                bookRepository.deleteBooks(sourceWithBooks.books)
                sourceRepository.deleteSource(sourceWithBooks.source)
            }

            return true
        } catch (ex: Exception) {
            return false
        }
    }
}

@Transaction
inline fun <reified U> SensayStore.runInTransaction(tx: () -> U): U = tx()
