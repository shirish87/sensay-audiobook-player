package data

import data.entity.Book
import data.entity.BookConfig
import data.entity.BookId
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.BookWithChapters
import data.entity.Bookmark
import data.entity.Chapter
import kotlinx.coroutines.flow.Flow

interface DataStore {

    fun booksProgressWithBookAndChapters(
        bookCategories: Collection<BookCategory>,
        filter: String,
        authorsFilter: List<String>,
        orderBy: String,
        isAscending: Boolean,
    ): Flow<List<BookProgressWithBookAndChapters>>

    fun bookAuthors(bookCategories: Collection<BookCategory>): Flow<List<String>>

    fun progressRestorableCount(): Flow<Int>

    suspend fun updateBookCategory(
        book: Book,
        chaptersList: List<Chapter>,
        bookProgress: BookProgress,
        bookCategory: BookCategory,
    ): Int

    suspend fun updateBookProgress(
        bookProgressVisibility: BookProgressVisibility,
    ): Int

    fun bookWithChapters(
        bookId: BookId,
    ): Flow<BookWithChapters>

    fun bookProgress(
        bookId: BookId,
    ): Flow<BookProgress>

    suspend fun createBookmark(bookmark: Bookmark): Long

    suspend fun deleteBookmark(bookmark: Bookmark): Int

    suspend fun updateBookConfig(bookConfig: BookConfig): Int
}
