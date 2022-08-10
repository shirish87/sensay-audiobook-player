package data.dao

import androidx.room.*
import data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao : BaseDao<Tag> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookWithTags(book: Book, tags: List<Tag>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookTagCrossRef(ref: BookTagCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookTagCrossRefs(refs: Collection<BookTagCrossRef>): Array<Long>

    @Delete
    suspend fun deleteBookTagCrossRef(ref: BookTagCrossRef): Int

    @Delete
    suspend fun deleteBookTagCrossRefs(refs: Collection<BookTagCrossRef>): Int

    @Query("SELECT * FROM Tag")
    fun tags(): Flow<List<Tag>>

    @Transaction
    @Query("SELECT * FROM Tag WHERE tagId = :tagId")
    fun tagWithBooks(tagId: Long): Flow<TagWithBooks>

    @Transaction
    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookWithTags(bookId: Long): Flow<BookWithTags>

    @Transaction
    @Query("SELECT * FROM Tag")
    fun tagsWithBooks(): Flow<List<TagWithBooks>>

    @Transaction
    @Query("SELECT * FROM Book")
    fun booksWithTags(): Flow<List<BookWithTags>>

    @Query("SELECT COUNT(*) FROM Tag")
    fun tagsCount(): Flow<Int>
}
