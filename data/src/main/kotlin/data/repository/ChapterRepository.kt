package data.repository

import android.net.Uri
import data.dao.ChapterDao
import data.dao.DeleteByBookId
import data.entity.BookId
import data.entity.Chapter
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {

    fun chaptersByUri(uri: Uri) = chapterDao.chaptersByUri(uri)

    fun chaptersMaxLastModifiedByUri(uri: Uri): Flow<Instant> =
        chapterDao.chaptersMaxLastModifiedByUri(uri)

    fun chaptersCount() = chapterDao.chaptersCount()

    fun bookWithChapters(bookId: BookId) = chapterDao.bookWithChapters(bookId)

    suspend fun createChapters(chapters: Collection<Chapter>) = chapterDao.insertAll(chapters)

    fun deleteChapters(vararg bookId: DeleteByBookId) = chapterDao.deleteByBookId(*bookId)

}
