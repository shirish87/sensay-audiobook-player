package data.repository

import data.dao.ShelfDao
import data.entity.BookId
import data.entity.BookShelfCrossRef
import kotlinx.coroutines.flow.firstOrNull
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

    suspend fun deleteShelves(bookIds: Collection<BookId>) {
        shelfDao.booksShelvesCrossRefs(bookIds).firstOrNull()?.let {
            shelfDao.deleteBookShelfCrossRefs(it)

            shelfDao.shelves(it.map { o -> o.shelfId }).firstOrNull()?.let { l ->
                shelfDao.deleteAll(l)
            }
        }
    }
}
