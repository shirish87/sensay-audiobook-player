package data.dao

import androidx.room.*
import data.entity.BookChapterCrossRef
import data.entity.BookWithChapters
import data.entity.Chapter
import data.entity.ChapterWithBook
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao : BaseDao<Chapter> {
    @Query("SELECT * FROM BookChapterCrossRef WHERE bookId IN (:bookIds)")
    fun booksChapterCrossRefs(bookIds: Collection<Long>): Flow<List<BookChapterCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookChapterCrossRefs(refs: Collection<BookChapterCrossRef>): Array<Long>

    @Delete
    suspend fun deleteBookChapterCrossRefs(refs: Collection<BookChapterCrossRef>): Int

    @Query("SELECT * FROM Chapter")
    fun chapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM Chapter WHERE chapterId IN (:chapterIds)")
    fun chapters(chapterIds: Collection<Long>): Flow<List<Chapter>>

    @Query("SELECT COUNT(*) FROM Chapter")
    fun chaptersCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM Chapter WHERE chapterId = :chapterId")
    fun chapterWithBook(chapterId: Long): Flow<ChapterWithBook>

    @Transaction
    @Query("SELECT * FROM Book")
    fun booksWithChapters(): Flow<List<BookWithChapters>>

    @Transaction
    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookWithChapters(bookId: Long): Flow<BookWithChapters>

    @Transaction
    @Query("SELECT * FROM Book WHERE uri = :uri")
    fun bookWithChaptersByUri(uri: String): Flow<BookWithChapters>
}
