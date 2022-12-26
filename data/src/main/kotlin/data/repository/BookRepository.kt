package data.repository

import android.net.Uri
import data.dao.BookDao
import data.entity.Book
import data.entity.BookId
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class BookRepository @Inject constructor(
    private val bookDao: BookDao,
) {

    fun bookById(bookId: BookId) = bookDao.bookById(bookId)

    fun bookByUri(uri: Uri) = bookDao.bookByUri(uri)

    fun booksCount() = bookDao.booksCount()

    suspend fun createBook(book: Book) = bookDao.insert(book)

    suspend fun updateBook(book: Book) = bookDao.update(book)

    suspend fun createBooks(books: Collection<Book>) = bookDao.insertAll(books)

    suspend fun deleteBooks(books: Collection<Book>) = bookDao.deleteAll(books)
}
