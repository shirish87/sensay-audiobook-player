package data.dao

import android.net.Uri
import androidx.room.*
import data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao : BaseDao<Chapter> {

    @Query("SELECT * FROM Chapter WHERE uri = :uri")
    fun chaptersByUri(uri: Uri): Flow<List<Chapter>>

    @Query("SELECT * FROM Chapter")
    fun chapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM Chapter WHERE chapterId IN (:chapterIds)")
    fun chapters(chapterIds: Collection<ChapterId>): Flow<List<Chapter>>

    @Query("SELECT COUNT(*) FROM Chapter")
    fun chaptersCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM Book")
    fun booksWithChapters(): Flow<List<BookWithChapters>>

    @Transaction
    @Query("SELECT * FROM Book WHERE bookId = :bookId")
    fun bookWithChapters(bookId: BookId): Flow<BookWithChapters>

    @Transaction
    @Query("SELECT * FROM Book WHERE uri = :uri")
    fun bookWithChaptersByUri(uri: String): Flow<BookWithChapters>

    @Delete(entity = Chapter::class)
    fun deleteByBookId(vararg deleteByBookId: DeleteByBookId)
}

data class DeleteByBookId(
    val bookId: BookId,
)
