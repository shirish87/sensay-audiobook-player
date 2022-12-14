package data.repository

import android.net.Uri
import data.dao.ChapterDao
import data.entity.BookChapterCrossRef
import data.entity.BookId
import data.entity.Chapter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {

    fun chaptersByUri(uri: Uri) = chapterDao.chaptersByUri(uri)

    fun chaptersCount() = chapterDao.chaptersCount()

    fun bookWithChapters(bookId: BookId) = chapterDao.bookWithChapters(bookId)

    suspend fun createChapters(chapters: Collection<Chapter>) = chapterDao.insertAll(chapters)

    suspend fun insertBookChapterCrossRefs(refs: Collection<BookChapterCrossRef>) =
        chapterDao.insertBookChapterCrossRefs(refs)

    suspend fun deleteChapters(bookIds: Collection<BookId>, batchSize: Int = 25) {
        chapterDao.booksChapterCrossRefs(bookIds).firstOrNull()?.let { refs ->
            refs.chunked(batchSize) {
                runBlocking {
                    chapterDao.deleteBookChapterCrossRefs(it)

                    chapterDao.chapters(it.map { o -> o.chapterId }).firstOrNull()?.let { l ->
                        chapterDao.deleteAll(l)
                    }
                }
            }
        }
    }
}
