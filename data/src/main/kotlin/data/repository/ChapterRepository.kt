package data.repository

import data.dao.ChapterDao
import data.entity.BookChapterCrossRef
import data.entity.Chapter
import javax.inject.Inject

class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {
    fun chaptersCount() = chapterDao.chaptersCount()

    fun booksWithChapters() = chapterDao.booksWithChapters()

    suspend fun createChapters(chapters: Collection<Chapter>) = chapterDao.insertAll(chapters)

    suspend fun insertBookChapterCrossRefs(refs: Collection<BookChapterCrossRef>) =
        chapterDao.insertBookChapterCrossRefs(refs)
}
