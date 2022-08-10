package data.repository

import data.BookCategory
import data.dao.BookProgressDao
import data.entity.BookProgress
import javax.inject.Inject

class BookProgressRepository @Inject constructor(
    private val bookProgressDao: BookProgressDao,
) {
    fun bookProgressCount() = bookProgressDao.bookProgressCount()

    fun booksProgressWithBookAndShelves(bookCategory: BookCategory) =
        bookProgressDao.booksProgressWithBookAndShelves(bookCategory)

    fun bookProgressWithBookAndChapters(bookId: Long) =
        bookProgressDao.bookProgressWithBookAndChapters(bookId)

    suspend fun insertBookProgress(bookProgress: BookProgress) =
        bookProgressDao.insert(bookProgress)
}
