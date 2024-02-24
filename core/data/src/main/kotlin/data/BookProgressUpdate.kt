package data

import data.entity.BookProgressId
import data.entity.ChapterId
import data.util.ContentDuration
import java.time.Instant

data class BookProgressUpdate(
    val bookProgressId: BookProgressId,
    val chapterId: ChapterId,

    val currentChapter: Int,
    val chapterProgress: ContentDuration,
    val chapterTitle: String,

    val bookProgress: ContentDuration,
    val bookRemaining: ContentDuration,

    val bookCategory: BookCategory,
    val lastUpdatedAt: Instant = Instant.now(),
)
