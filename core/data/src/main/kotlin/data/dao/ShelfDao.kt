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

    @JvmSuppressWildcards
    @Query("SELECT * FROM BookShelfCrossRef WHERE bookId IN (:bookIds)")
    fun booksShelvesCrossRefs(bookIds: Collection<BookId>): Flow<List<BookShelfCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookShelfCrossRefs(refs: Collection<BookShelfCrossRef>): Array<Long>

    @Delete
    suspend fun deleteBookShelfCrossRefs(refs: Collection<BookShelfCrossRef>): Int

    @JvmSuppressWildcards
    @Query("SELECT * FROM Shelf ORDER BY sortOrder")
    fun shelves(): Flow<List<Shelf>>

    @JvmSuppressWildcards
    @Query("SELECT * FROM Shelf WHERE shelfId IN (:shelfIds)")
    fun shelves(shelfIds: Collection<ShelfId>): Flow<List<Shelf>>

    @Transaction
    @Query("SELECT * FROM Shelf WHERE shelfId = :shelfId")
    fun shelfWithBooks(shelfId: ShelfId): Flow<ShelfWithBooks>

    @Transaction
    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookWithShelves(bookId: BookId): Flow<BooksWithShelves>

    @Transaction
    @Query("SELECT * FROM Book")
    fun booksWithShelves(): Flow<BooksWithShelves>

    @Transaction
    @Query("SELECT * FROM Shelf ORDER BY sortOrder")
    fun shelvesWithBooks(): Flow<List<ShelfWithBooks>>

    @Query("SELECT COUNT(*) FROM Shelf")
    fun shelvesCount(): Flow<Int>
}
