package data.repository

import data.dao.BookConfigDao
import data.entity.BookConfig
import data.entity.BookId
import javax.inject.Inject

class BookConfigRepository @Inject constructor(
    private val bookConfigDao: BookConfigDao,
) {

    fun bookConfig(bookId: BookId) = bookConfigDao.bookConfig(bookId)

    suspend fun insertBookConfig(bookConfig: BookConfig) = bookConfigDao.insert(bookConfig)

    suspend fun updateBookConfig(bookConfig: BookConfig) = bookConfigDao.update(bookConfig)
}
