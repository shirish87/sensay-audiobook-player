package data.repository

import data.BookCategory
import data.dao.BookProgressDao
import data.entity.BookProgress
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class BookProgressRepository @Inject constructor(
    private val bookProgressDao: BookProgressDao,
) {
    fun bookProgressCount() = bookProgressDao.bookProgressCount()

    fun booksProgressWithBookAndShelves(bookCategory: BookCategory) =
        bookProgressDao.booksProgressWithBookAndShelves(bookCategory)

    fun booksProgressWithBookAndChapters() =
        bookProgressDao.booksProgressWithBookAndChapters()

    fun bookProgressWithBookAndChapters(bookId: Long) =
        bookProgressDao.bookProgressWithBookAndChapters(bookId)

    suspend fun insertBookProgress(bookProgress: BookProgress) =
        bookProgressDao.insert(bookProgress)

    suspend fun deleteBooksProgress(bookIds: Collection<Long>) {
        bookProgressDao.booksProgress(bookIds).firstOrNull()?.let {
            bookProgressDao.deleteAll(it)
        }
    }
}
