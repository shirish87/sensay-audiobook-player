package com.dotslashlabs.sensay.common

import android.net.Uri
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewState
import data.BookCategory
import data.entity.*
import data.util.ContentDuration
import java.time.Instant

data class BookProgressWithDuration(
    val mediaId: MediaId,
    val bookProgressId: BookProgressId,
    val bookId: BookId,
    val chapterId: ChapterId,
    val bookTitle: String,
    val chapterTitle: String,
    val series: String?,
    val author: String?,
    val coverUri: Uri?,
    val totalChapters: Int,
    val currentChapter: Int = 0,
    val bookDuration: ContentDuration = ContentDuration.ZERO,
    val chapterStart: ContentDuration = ContentDuration.ZERO,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,
    val chapterDuration: ContentDuration = ContentDuration.ZERO,
    val lastUpdatedAt: Instant = Instant.now(),
) {

    companion object {

        fun fromBookAndChapters(
            bookProgress: BookProgress,
            book: Book,
            chapters: List<Chapter>,
        ): List<BookProgressWithDuration> {

            return chapters
                .sortedBy { c -> c.trackId }
                .mapIndexed { idx, chapter ->
                    val mediaId = PlayerViewState.getMediaId(book.bookId, chapter.chapterId)

                    val chapterProgress = if (chapter.chapterId == bookProgress.chapterId) {
                        bookProgress.chapterProgress
                    } else ContentDuration.ZERO

                    BookProgressWithDuration(
                        mediaId = mediaId,
                        bookProgressId = bookProgress.bookProgressId,
                        bookId = book.bookId,
                        chapterId = chapter.chapterId,
                        bookTitle = book.title,
                        chapterTitle = chapter.title,
                        series = book.series,
                        author = chapter.author ?: book.author,
                        coverUri = chapter.coverUri ?: book.coverUri,
                        totalChapters = chapters.size,
                        currentChapter = idx + 1,
                        chapterStart = chapter.start,
                        chapterProgress = chapterProgress,
                        chapterDuration = chapter.duration,
                        bookDuration = book.duration,
                    )
                }
        }
    }

    val bookProgress: ContentDuration = ContentDuration.ms(chapterStart.ms + chapterProgress.ms)

    val bookCategory = when (bookProgress.ms) {
        0L -> BookCategory.NOT_STARTED
        bookDuration.ms -> BookCategory.FINISHED
        else -> BookCategory.CURRENT
    }

    fun toBookProgress(chapterPosition: Long): BookProgress {
        val bookProgressMs = chapterStart.ms + chapterPosition

        return BookProgress(
            chapterProgress = ContentDuration.ms(chapterPosition),
            bookProgress = ContentDuration.ms(bookProgressMs),
            bookProgressId = bookProgressId,
            bookId = bookId,
            chapterId = chapterId,
            totalChapters = totalChapters,
            currentChapter = currentChapter,
            bookCategory = when (bookProgressMs) {
                0L -> BookCategory.NOT_STARTED
                bookDuration.ms -> BookCategory.FINISHED
                else -> BookCategory.CURRENT
            },
            lastUpdatedAt = Instant.now(),
        )
    }
}
