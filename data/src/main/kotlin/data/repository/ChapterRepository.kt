package data.repository

import android.net.Uri
import data.dao.ChapterDao
import data.dao.DeleteByBookId
import data.entity.BookId
import data.entity.Chapter
import javax.inject.Inject

class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {

    fun chaptersByUri(uri: Uri) = chapterDao.chaptersByUri(uri)

    fun chaptersCount() = chapterDao.chaptersCount()

    fun bookWithChapters(bookId: BookId) = chapterDao.bookWithChapters(bookId)

    suspend fun createChapters(chapters: Collection<Chapter>) = chapterDao.insertAll(chapters)

    suspend fun deleteChapters(vararg bookId: DeleteByBookId) = chapterDao.deleteByBookId(*bookId)

}
