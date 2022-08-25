package com.dotslashlabs.sensay.common

import android.net.Uri
import data.BookCategory
import data.entity.BookId
import data.entity.BookProgress
import data.entity.BookProgressId
import data.entity.ChapterId
import data.util.ContentDuration
import java.time.Instant

data class BookProgressWithDuration(
    val bookProgressId: BookProgressId,
    val bookId: BookId,
    val chapterId: ChapterId,
    val bookTitle: String,
    val chapterTitle: String,
    val author: String?,
    val series: String?,
    val coverUri: Uri?,
    val totalChapters: Int,
    val currentChapter: Int = 0,
    val bookDuration: ContentDuration = ContentDuration.ZERO,
    val chapterStart: ContentDuration = ContentDuration.ZERO,
    val chapterProgress: ContentDuration = ContentDuration.ZERO,
    val chapterDuration: ContentDuration = ContentDuration.ZERO,
    val bookCategory: BookCategory = BookCategory.NOT_STARTED,
    val lastUpdatedAt: Instant = Instant.now(),
) {

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
