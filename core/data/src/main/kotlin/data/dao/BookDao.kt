package data.dao

import android.net.Uri
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import data.InactiveReason
import data.entity.Book
import data.entity.BookId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface BookDao : BaseDao<Book> {
    @Query("SELECT * FROM Book")
    fun books(): Flow<List<Book>>

    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookById(bookId: BookId): Flow<Book>

//    @Query("SELECT * FROM Book WHERE uri = :uri")
//    fun bookByUri(uri: Uri): Flow<Book>

    @Query("SELECT * FROM Book WHERE hash = :hash")
    fun bookByHash(hash: String): Flow<Book>

    @Query("SELECT COUNT(*) FROM Book")
    fun booksCount(): Flow<Int>

    @Update(entity = Book::class)
    suspend fun updateBook(vararg bookScanUpdate: BookScanUpdate): Int

    @Update(entity = Book::class)
    suspend fun updateBook(vararg bookCoverScanUpdate: BookCoverScanUpdate): Int
}

data class BookScanUpdate(
    val bookId: BookId,
    val isActive: Boolean,
    val inactiveReason: InactiveReason? = null,
    val scanInstant: Instant? = null,
)

data class BookCoverScanUpdate(
    val bookId: BookId,
)
