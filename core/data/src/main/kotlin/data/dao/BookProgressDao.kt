package data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import data.BookCategory
import data.BookProgressUpdate
import data.BookProgressVisibility
import data.entity.BookId
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.BookProgressWithBookAndShelves
import kotlinx.coroutines.flow.Flow

@Dao
interface BookProgressDao : BaseDao<BookProgress> {
    @Query("SELECT COUNT(*) FROM BookProgress")
    fun bookProgressCount(): Flow<Int>

    @JvmSuppressWildcards
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgress(bookId: BookId): Flow<BookProgress>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookCategory = :bookCategory")
    fun booksProgressWithBookAndShelves(
        bookCategory: BookCategory,
    ): Flow<List<BookProgressWithBookAndShelves>>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndShelves(bookId: BookId): Flow<BookProgressWithBookAndShelves>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId = :bookId")
    fun bookProgressWithBookAndChapters(bookId: BookId): Flow<BookProgressWithBookAndChapters>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId IN (:bookIds)")
    fun bookProgressWithBookAndChapters(
        bookIds: Collection<BookId>,
    ): Flow<List<BookProgressWithBookAndChapters>>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress ORDER BY bookAuthor, bookTitle")
    fun booksProgressWithBookAndChapters(): Flow<List<BookProgressWithBookAndChapters>>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookCategory IN (:bookCategories)")
    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
    ): Flow<List<BookProgressWithBookAndChapters>>

    @Transaction
    @Query("""
        SELECT DISTINCT bookAuthor
        FROM BookProgress
        WHERE
            bookCategory IN (:bookCategories)
        ORDER BY bookAuthor
    """)
    fun bookAuthors(
        bookCategories: Collection<BookCategory>,
    ): Flow<List<String>>

    @Transaction
    @Query("""
        SELECT DISTINCT bookSeries
        FROM BookProgress
        WHERE
            bookCategory IN (:bookCategories)
        ORDER BY bookSeries
    """)
    fun bookSeries(
        bookCategories: Collection<BookCategory>,
    ): Flow<List<String>>

    @JvmSuppressWildcards
    @Transaction
    @Query(
        """
        SELECT *
        FROM BookProgress
        WHERE
            bookCategory IN (:bookCategories)
            AND (
                bookTitle LIKE :filter
                OR bookAuthor LIKE :filter
            )
        ORDER BY
            CASE WHEN lower(:orderBy) = lower('bookTitle') AND :isAscending THEN bookTitle END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookTitle') AND NOT :isAscending THEN bookTitle END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('chapterTitle') AND :isAscending THEN chapterTitle END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('chapterTitle') AND NOT :isAscending THEN chapterTitle END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookAuthor') AND :isAscending THEN bookAuthor END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookAuthor') AND NOT :isAscending THEN bookAuthor END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookSeries') AND :isAscending THEN bookSeries END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookSeries') AND NOT :isAscending THEN bookSeries END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookRemaining') AND :isAscending THEN bookRemaining END ASC,
            CASE WHEN lower(:orderBy) = lower('bookRemaining') AND NOT :isAscending THEN bookRemaining END DESC,

            CASE WHEN lower(:orderBy) = lower('createdAt') AND :isAscending THEN createdAt END ASC,
            CASE WHEN lower(:orderBy) = lower('createdAt') AND NOT :isAscending THEN createdAt END DESC,

            CASE WHEN lower(:orderBy) = lower('lastUpdatedAt') AND :isAscending THEN lastUpdatedAt END ASC,
            CASE WHEN lower(:orderBy) = lower('lastUpdatedAt') AND NOT :isAscending THEN lastUpdatedAt END DESC
    """
    )
    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        orderBy: String,
        isAscending: Boolean,
    ): Flow<List<BookProgressWithBookAndChapters>>

    @JvmSuppressWildcards
    @Transaction
    @Query(
        """
        SELECT *
        FROM BookProgress
        WHERE
            bookCategory IN (:bookCategories)
            AND (
                bookTitle LIKE :filter
                OR bookAuthor LIKE :filter
            )
            AND bookAuthor IN (:authorsFilter)
        ORDER BY
            CASE WHEN lower(:orderBy) = lower('bookTitle') AND :isAscending THEN bookTitle END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookTitle') AND NOT :isAscending THEN bookTitle END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('chapterTitle') AND :isAscending THEN chapterTitle END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('chapterTitle') AND NOT :isAscending THEN chapterTitle END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookAuthor') AND :isAscending THEN bookAuthor END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookAuthor') AND NOT :isAscending THEN bookAuthor END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookSeries') AND :isAscending THEN bookSeries END COLLATE NOCASE ASC,
            CASE WHEN lower(:orderBy) = lower('bookSeries') AND NOT :isAscending THEN bookSeries END COLLATE NOCASE DESC,

            CASE WHEN lower(:orderBy) = lower('bookRemaining') AND :isAscending THEN bookRemaining END ASC,
            CASE WHEN lower(:orderBy) = lower('bookRemaining') AND NOT :isAscending THEN bookRemaining END DESC,

            CASE WHEN lower(:orderBy) = lower('createdAt') AND :isAscending THEN createdAt END ASC,
            CASE WHEN lower(:orderBy) = lower('createdAt') AND NOT :isAscending THEN createdAt END DESC,

            CASE WHEN lower(:orderBy) = lower('lastUpdatedAt') AND :isAscending THEN lastUpdatedAt END ASC,
            CASE WHEN lower(:orderBy) = lower('lastUpdatedAt') AND NOT :isAscending THEN lastUpdatedAt END DESC
    """
    )
    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        authorsFilter: List<String>,
        orderBy: String,
        isAscending: Boolean,
    ): Flow<List<BookProgressWithBookAndChapters>>

    @JvmSuppressWildcards
    @Transaction
    @Query("SELECT * FROM BookProgress WHERE bookId IN (:bookIds)")
    fun booksProgress(bookIds: Collection<BookId>): Flow<List<BookProgress>>

    @JvmSuppressWildcards
    @Update(entity = BookProgress::class)
    override suspend fun update(entity: BookProgress): Int

    @JvmSuppressWildcards
    @Update(entity = BookProgress::class)
    suspend fun update(bookProgressUpdate: BookProgressUpdate): Int

    @JvmSuppressWildcards
    @Update(entity = BookProgress::class)
    suspend fun update(bookProgressVisibility: BookProgressVisibility): Int
}
