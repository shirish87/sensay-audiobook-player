package data.repository

import android.net.Uri
import data.InactiveReason
import data.dao.BookCoverScanUpdate
import data.dao.BookDao
import data.dao.BookScanUpdate
import data.entity.Book
import data.entity.BookId
import java.time.Instant
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val bookDao: BookDao,
) {

    fun bookById(bookId: BookId) = bookDao.bookById(bookId)

    fun bookByHash(hash: String) = bookDao.bookByHash(hash)

    fun booksCount() = bookDao.booksCount()

    suspend fun createBook(book: Book) = bookDao.insert(book)

    suspend fun updateBook(book: Book) = bookDao.update(book)

    suspend fun updateBook(
        bookId: BookId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
        scanInstant: Instant? = null,
    ): Int = bookDao.updateBook(
        BookScanUpdate(
            bookId,
            isActive,
            inactiveReason,
            scanInstant,
        ),
    )

    suspend fun createBooks(books: Collection<Book>) = bookDao.insertAll(books)

    suspend fun deleteBooks(books: Collection<Book>) = bookDao.deleteAll(books)
}
