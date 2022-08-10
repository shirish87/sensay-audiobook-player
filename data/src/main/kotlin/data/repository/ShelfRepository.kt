package data.repository

import data.dao.ShelfDao
import data.entity.BookShelfCrossRef
import javax.inject.Inject

class ShelfRepository @Inject constructor(
    private val shelfDao: ShelfDao,
) {
    fun shelves() = shelfDao.shelves()
    fun shelvesCount() = shelfDao.shelvesCount()

    suspend fun insertBookShelfCrossRef(ref: BookShelfCrossRef) =
        shelfDao.insertBookShelfCrossRef(ref)

    suspend fun insertBookShelfCrossRefs(refs: Collection<BookShelfCrossRef>) =
        shelfDao.insertBookShelfCrossRefs(refs)
}
