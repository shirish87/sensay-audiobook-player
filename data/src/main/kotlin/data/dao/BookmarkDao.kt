package data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import data.entity.BookId
import data.entity.Bookmark
import data.entity.BookmarkWithChapter
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao : BaseDao<Bookmark> {
    @Query("SELECT * FROM Bookmark")
    fun bookmarks(): Flow<List<Bookmark>>

    @Transaction
    @Query("SELECT * FROM Bookmark WHERE bookId = :bookId")
    fun bookmarksWithChapters(bookId: BookId): Flow<List<BookmarkWithChapter>>

    @Query("SELECT COUNT(*) FROM Bookmark")
    fun bookmarksCount(): Flow<Int>

    @Transaction
    @Query("DELETE FROM Bookmark WHERE bookId IN (:bookIds)")
    suspend fun deleteBookmarksForBooks(bookIds: Collection<BookId>): Int
}
