package data.repository

import data.BookCategory
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

    suspend fun update(bookProgress: BookProgress) = bookProgressDao.update(bookProgress)
}
