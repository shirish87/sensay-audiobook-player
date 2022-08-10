package data.repository

import data.dao.TagDao
import data.entity.BookTagCrossRef
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
}
