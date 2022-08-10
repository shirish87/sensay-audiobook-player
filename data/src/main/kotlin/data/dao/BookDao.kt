package data.dao

import androidx.room.Dao
import androidx.room.Query
import data.entity.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao : BaseDao<Book> {
    @Query("SELECT * FROM Book")
    fun books(): Flow<List<Book>>

    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookById(bookId: Long): Flow<Book>

    @Query("SELECT * FROM Book WHERE uri = :uri")
    fun bookByUri(uri: String): Flow<Book>

    @Query("SELECT COUNT(*) FROM Book")
    fun booksCount(): Flow<Int>
}
