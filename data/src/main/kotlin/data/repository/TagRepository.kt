package data.repository

import data.dao.TagDao
import data.entity.BookId
import data.entity.BookTagCrossRef
import data.entity.Tag
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class TagRepository @Inject constructor(
    private val tagDao: TagDao,
) {
    fun tags() = tagDao.tags()
    fun tagsByNames(tagNames: Collection<String>) = tagDao.tagsByNames(tagNames)
    fun tagsCount() = tagDao.tagsCount()

    suspend fun createTags(tags: Collection<Tag>) = tagDao.insertAll(tags)

    suspend fun insertBookTagCrossRef(ref: BookTagCrossRef) =
        tagDao.insertBookTagCrossRef(ref)

    suspend fun insertBookTagCrossRefs(refs: Collection<BookTagCrossRef>) =
        tagDao.insertBookTagCrossRefs(refs)

    suspend fun deleteTags(bookIds: Collection<BookId>) {
        tagDao.booksTagsCrossRefs(bookIds).firstOrNull()?.let {
            tagDao.deleteBookTagCrossRefs(it)

            tagDao.tags(it.map { o -> o.tagId }).firstOrNull()?.let { l ->
                tagDao.deleteAll(l)
            }
        }
    }
}
