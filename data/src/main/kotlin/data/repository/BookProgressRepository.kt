package data.repository

import data.BookCategory
import data.BookProgressUpdate
import data.BookProgressVisibility
import data.dao.BookProgressDao
import data.dao.ProgressDao
import data.dao.insertOrUpdate
import data.entity.BookId
import data.entity.BookProgress
import data.entity.Progress
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class BookProgressRepository @Inject constructor(
    private val bookProgressDao: BookProgressDao,
    private val progressDao: ProgressDao,
) {
    fun bookProgressCount() = bookProgressDao.bookProgressCount()

    fun bookProgress(bookId: BookId) = bookProgressDao.bookProgress(bookId)

    fun booksProgressWithBookAndShelves(bookCategory: BookCategory) =
        bookProgressDao.booksProgressWithBookAndShelves(bookCategory)

    fun booksProgressWithBookAndChapters() =
        bookProgressDao.booksProgressWithBookAndChapters()

    fun booksProgressWithBookAndChapters(bookCategories: Collection<BookCategory>) =
        bookProgressDao.booksProgressWithBookAndChapters(bookCategories)

    fun bookAuthors(bookCategories: Collection<BookCategory>) =
        bookProgressDao.bookAuthors(bookCategories)

    fun bookSeries(bookCategories: Collection<BookCategory>) =
        bookProgressDao.bookSeries(bookCategories)

    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        authorsFilter: List<String>,
        orderBy: String,
        isAscending: Boolean,
    ) = if (authorsFilter.isEmpty()) {
        bookProgressDao.booksProgressWithBookAndChapters(
            bookCategories,
            filter,
            orderBy,
            isAscending,
        )
    } else {
        bookProgressDao.booksProgressWithBookAndChapters(
            bookCategories,
            filter,
            authorsFilter,
            orderBy,
            isAscending,
        )
    }

    fun bookProgressWithBookAndChapters(bookId: BookId) =
        bookProgressDao.bookProgressWithBookAndChapters(bookId)

    fun bookProgressWithBookAndChapters(bookIds: Collection<BookId>) =
        bookProgressDao.bookProgressWithBookAndChapters(bookIds)

    suspend fun insertBookProgress(bookProgress: BookProgress) =
        bookProgressDao.insert(bookProgress)

    suspend fun deleteOrResetBooksProgress(bookIds: Collection<BookId>, batchSize: Int = 25) {
        val results = bookProgressDao.booksProgress(bookIds).firstOrNull() ?: return
        if (results.isEmpty()) return

        // preserve progress for books that have some progress associated with them
        results.filter {
            it.bookProgress.ms > 0L && it.bookRemaining.ms > 0
        }.forEach { bookProgress ->
            progressDao.insertOrUpdate(Progress.from(bookProgress))
        }

        results.chunked(batchSize) {
            runBlocking { bookProgressDao.deleteAll(it) }
        }
    }

    suspend fun update(bookProgressUpdate: BookProgressUpdate) =
        bookProgressDao.update(bookProgressUpdate)

    suspend fun update(bookProgressVisibility: BookProgressVisibility) =
        bookProgressDao.update(bookProgressVisibility)
}
