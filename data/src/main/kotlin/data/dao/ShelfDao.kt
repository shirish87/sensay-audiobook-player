package data.dao

import androidx.room.*
import data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao : BaseDao<Shelf> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookWithShelves(book: Book, shelves: List<Shelf>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookShelfCrossRef(ref: BookShelfCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookShelfCrossRefs(refs: Collection<BookShelfCrossRef>): Array<Long>

    @Delete
    suspend fun deleteBookShelfCrossRef(ref: BookShelfCrossRef): Int

    @Delete
    suspend fun deleteBookShelfCrossRefs(refs: Collection<BookShelfCrossRef>): Int

    @Query("SELECT * FROM Shelf ORDER BY sortOrder")
    fun shelves(): Flow<List<Shelf>>

    @Transaction
    @Query("SELECT * FROM Shelf WHERE shelfId = :shelfId")
    fun shelfWithBooks(shelfId: Long): Flow<ShelfWithBooks>

    @Transaction
    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookWithShelves(bookId: Long): Flow<BooksWithShelves>

    @Transaction
    @Query("SELECT * FROM Book")
    fun booksWithShelves(): Flow<BooksWithShelves>

    @Transaction
    @Query("SELECT * FROM Shelf ORDER BY sortOrder")
    fun shelvesWithBooks(): Flow<List<ShelfWithBooks>>

    @Query("SELECT COUNT(*) FROM Shelf")
    fun shelvesCount(): Flow<Int>
}
