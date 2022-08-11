package data.repository

import data.dao.TagDao
import data.entity.BookTagCrossRef
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class TagRepository @Inject constructor(
    private val tagDao: TagDao,
) {
    fun tags() = tagDao.tags()
    fun tagsCount() = tagDao.tagsCount()

    suspend fun insertBookTagCrossRef(ref: BookTagCrossRef) =
        tagDao.insertBookTagCrossRef(ref)

    suspend fun insertBookTagCrossRefs(refs: Collection<BookTagCrossRef>) =
        tagDao.insertBookTagCrossRefs(refs)

    suspend fun deleteTags(bookIds: Collection<Long>) {
        tagDao.booksTagsCrossRefs(bookIds).firstOrNull()?.let {
            tagDao.deleteBookTagCrossRefs(it)

            tagDao.tags(it.map { o -> o.tagId }).firstOrNull()?.let { l ->
                tagDao.deleteAll(l)
            }
        }
    }
}
