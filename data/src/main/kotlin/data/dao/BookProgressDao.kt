package data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import data.BookCategory
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.BookProgressWithBookAndShelves
import kotlinx.coroutines.flow.Flow


@Dao
interface BookProgressDao : BaseDao<BookProgress> {
    @Query("SELECT COUNT(*) FROM BookProgress")
    fun bookProgressCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookCategory = :bookCategory")
    fun booksProgressWithBookAndShelves(
        bookCategory: BookCategory,
    ): Flow<List<BookProgressWithBookAndShelves>>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndShelves(bookId: Long): Flow<BookProgressWithBookAndShelves>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndShelvesPaged(bookId: Long): PagingSource<Int, BookProgressWithBookAndShelves>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndChapters(bookId: Long): Flow<BookProgressWithBookAndChapters>

    @Transaction
    @Query("SELECT * FROM BookProgress")
    fun booksProgressWithBookAndChapters(): Flow<List<BookProgressWithBookAndChapters>>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId IN (:bookIds)")
    fun booksProgress(bookIds: Collection<Long>): Flow<List<BookProgress>>
}
