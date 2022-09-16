package data.repository

import data.BookCategory
import data.BookProgressUpdate
import data.dao.BookProgressDao
import data.entity.BookId
import data.entity.BookProgress
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class BookProgressRepository @Inject constructor(
    private val bookProgressDao: BookProgressDao,
) {
    fun bookProgressCount() = bookProgressDao.bookProgressCount()

    fun bookProgress(bookId: BookId) = bookProgressDao.bookProgress(bookId)

    fun booksProgressWithBookAndShelves(bookCategory: BookCategory) =
        bookProgressDao.booksProgressWithBookAndShelves(bookCategory)

    fun booksProgressWithBookAndChapters() =
        bookProgressDao.booksProgressWithBookAndChapters()

    fun booksProgressWithBookAndChapters(bookCategories: Collection<BookCategory>) =
        bookProgressDao.booksProgressWithBookAndChapters(bookCategories)

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

    suspend fun deleteBooksProgress(bookIds: Collection<BookId>) {
        bookProgressDao.booksProgress(bookIds).firstOrNull()?.let {
            bookProgressDao.deleteAll(it)
        }
    }

    suspend fun update(bookProgressUpdate: BookProgressUpdate) =
        bookProgressDao.update(bookProgressUpdate)
}
