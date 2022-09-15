package com.dotslashlabs.sensay.common

import android.net.Uri
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewState
import data.BookCategory
import data.BookProgressUpdate
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
    val bookDuration: ContentDuration,
    val bookChapterStart: ContentDuration,
    val chapterStart: ContentDuration = ContentDuration.ZERO,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,
    val chapterDuration: ContentDuration = ContentDuration.ZERO,
) {

    companion object {

        fun fromBookAndChapters(
            bookProgress: BookProgress,
            book: Book,
            chapters: List<Chapter>,
        ): List<BookProgressWithDuration> {

            val chaptersSorted = chapters
                .sortedBy { c -> c.trackId }

            return chaptersSorted
                .mapIndexed { idx, chapter ->
                    val mediaId = PlayerViewState.getMediaId(book.bookId, chapter.chapterId)

                    val chapterProgress = if (chapter.chapterId == bookProgress.chapterId) {
                        bookProgress.chapterProgress
                    } else ContentDuration.ZERO

                    val bookChapterStartMs = chaptersSorted.subList(0, idx).sumOf { it.duration.ms }

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
                        bookChapterStart = ContentDuration.ms(bookChapterStartMs),
                    )
                }
        }
    }

    val bookProgress: ContentDuration =
        ContentDuration.ms(minOf(bookDuration.ms, bookChapterStart.ms + chapterProgress.ms))

    fun toBookProgressUpdate(chapterPosition: Long): BookProgressUpdate {
        val bookProgressMs = minOf(bookDuration.ms, bookChapterStart.ms + chapterPosition)

        return BookProgressUpdate(
            bookProgressId = bookProgressId,

            currentChapter = currentChapter,
            chapterProgress = ContentDuration.ms(chapterPosition),
            chapterTitle = chapterTitle,

            bookProgress = ContentDuration.ms(bookProgressMs),
            bookRemaining = ContentDuration.ms(maxOf(0, bookDuration.ms - bookProgressMs)),

            bookCategory = when (bookProgressMs) {
                0L -> BookCategory.NOT_STARTED
                bookDuration.ms -> BookCategory.FINISHED
                else -> BookCategory.CURRENT
            },
            lastUpdatedAt = Instant.now(),
        )
    }
}

