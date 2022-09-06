package data.repository

import data.dao.BookmarkDao
import data.entity.BookId
import data.entity.Bookmark
import javax.inject.Inject

class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) {

    fun bookmarksCount() = bookmarkDao.bookmarksCount()

    fun bookmarksWithChapters(bookId: BookId) = bookmarkDao.bookmarksWithChapters(bookId)

    suspend fun createBookmark(bookmark: Bookmark) = bookmarkDao.insert(bookmark)

    suspend fun createBookmarks(bookmarks: Collection<Bookmark>) = bookmarkDao.insertAll(bookmarks)

    suspend fun deleteBookmarks(bookmarks: Collection<Bookmark>) = bookmarkDao.deleteAll(bookmarks)
}
