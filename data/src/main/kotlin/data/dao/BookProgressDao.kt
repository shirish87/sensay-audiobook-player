package data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import data.BookCategory
import data.entity.BookId
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.BookProgressWithBookAndShelves
import kotlinx.coroutines.flow.Flow


@Dao
interface BookProgressDao : BaseDao<BookProgress> {
    @Query("SELECT COUNT(*) FROM BookProgress")
    fun bookProgressCount(): Flow<Int>

    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgress(bookId: BookId): Flow<BookProgress>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookCategory = :bookCategory")
    fun booksProgressWithBookAndShelves(
        bookCategory: BookCategory,
    ): Flow<List<BookProgressWithBookAndShelves>>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndShelves(bookId: BookId): Flow<BookProgressWithBookAndShelves>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndShelvesPaged(bookId: BookId): PagingSource<Int, BookProgressWithBookAndShelves>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndChapters(bookId: BookId): Flow<BookProgressWithBookAndChapters>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId IN (:bookIds)")
    fun bookProgressWithBookAndChapters(bookIds: Collection<BookId>): Flow<List<BookProgressWithBookAndChapters>>

    @Transaction
    @Query("SELECT * FROM BookProgress")
    fun booksProgressWithBookAndChapters(): Flow<List<BookProgressWithBookAndChapters>>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookCategory IN (:bookCategories)")
    fun booksProgressWithBookAndChapters(bookCategories: Collection<BookCategory>): Flow<List<BookProgressWithBookAndChapters>>

    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId IN (:bookIds)")
    fun booksProgress(bookIds: Collection<BookId>): Flow<List<BookProgress>>
}
