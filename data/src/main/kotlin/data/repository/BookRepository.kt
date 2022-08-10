package data.repository

import data.dao.BookDao
import data.dao.runInTransaction
import data.entity.Book
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val bookDao: BookDao,
) {
    fun booksCount() = bookDao.booksCount()

    suspend fun createBooks(
        books: Collection<Book>,
        runInTx: suspend (bookIds: List<Long>) -> Unit
    ): List<Long> {

        return bookDao.runInTransaction {
            val bookIds = bookDao.insertAll(books)
            runInTx(bookIds)
            bookIds
        }
    }
}
