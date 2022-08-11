package data.repository

import data.dao.ChapterDao
import data.entity.BookChapterCrossRef
import data.entity.Chapter
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {
    fun chaptersCount() = chapterDao.chaptersCount()

    suspend fun createChapters(chapters: Collection<Chapter>) = chapterDao.insertAll(chapters)

    suspend fun insertBookChapterCrossRefs(refs: Collection<BookChapterCrossRef>) =
        chapterDao.insertBookChapterCrossRefs(refs)

    suspend fun deleteChapters(bookIds: Collection<Long>) {
        chapterDao.booksChapterCrossRefs(bookIds).firstOrNull()?.let { it ->
            chapterDao.deleteBookChapterCrossRefs(it)

            chapterDao.chapters(it.map { o -> o.chapterId }).firstOrNull()?.let { l ->
                chapterDao.deleteAll(l)
            }
        }
    }
}
