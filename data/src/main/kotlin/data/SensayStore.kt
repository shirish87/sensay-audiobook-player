package data

import android.net.Uri
import data.entity.BookChapterCrossRef
import data.entity.BookProgress
import data.entity.BookWithChapters
import data.repository.*
import javax.inject.Inject

class SensayStore @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val shelfRepository: ShelfRepository,
    private val tagRepository: TagRepository,
    private val bookProgressRepository: BookProgressRepository,
) {

    suspend fun createBooksWithChapters(booksWithChapters: List<BookWithChapters>) =
        bookRepository.createBooks(booksWithChapters.map { it.book }) { bookIds ->
            bookIds.forEachIndexed { index, bookId ->
                val chapters = booksWithChapters[index].chapters.sortedBy { it.trackId }
                if (chapters.isEmpty()) {
                    return@forEachIndexed
                }

                val chapterIds = chapterRepository.createChapters(chapters)
                if (chapterIds.isEmpty()) {
                    return@forEachIndexed
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
            }
        }

    fun booksProgressWithBookAndChapters() = bookProgressRepository.booksProgressWithBookAndChapters()

    fun booksCount() = bookRepository.booksCount()

    fun booksByUri(uri: Uri) = bookRepository.booksByUri(uri)

    fun chaptersCount() = chapterRepository.chaptersCount()
    fun shelvesCount() = shelfRepository.shelvesCount()
    fun tagsCount() = tagRepository.tagsCount()
    fun bookProgressCount() = bookProgressRepository.bookProgressCount()
}
