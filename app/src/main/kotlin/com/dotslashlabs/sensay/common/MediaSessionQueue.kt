package com.dotslashlabs.sensay.common

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.dotslashlabs.sensay.util.bookId
import com.dotslashlabs.sensay.util.chapterId
import com.dotslashlabs.sensay.util.toMediaItem
import com.google.common.util.concurrent.ListenableFuture
import data.SensayStore
import data.entity.BookId
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus

typealias MediaId = String

class MediaSessionQueue(private val store: SensayStore) {

    private val scope = MainScope() + CoroutineName(this::class.java.simpleName)

    private val mediaItemsCache: MutableMap<MediaId, BookProgressWithDuration> = mutableMapOf()

    val mediaSessionCallback = object : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {

            mediaSession.player.clearMediaItems()
            mediaItemsCache.clear()

            val bookIds = mediaItems.mapNotNull { it.bookId }.toSet()

            val bookProgressMap: Map<BookId, BookProgressWithBookAndChapters> =
                store.bookProgressWithBookAndChapters(bookIds)
                    .first()
                    .associateBy { it.book.bookId }

            mediaItems.fold(mutableListOf()) { acc, it ->
                val bookId = it.bookId ?: return@fold acc
                val chapterId = it.chapterId ?: return@fold acc
                val bookProgress = bookProgressMap[bookId] ?: return@fold acc
                val mediaItem = bookProgress.toMediaItem(it, chapterId) ?: return@fold acc

                val (progress, book, _, chapters) = bookProgress
                val chaptersSorted = chapters.sortedBy { c -> c.trackId }

                val chapterIndex = chaptersSorted.indexOfFirst { c -> c.chapterId == chapterId }
                val chapter = chaptersSorted[chapterIndex]

                val bookChapterStartMs = chaptersSorted.subList(0, chapterIndex)
                    .sumOf { it.duration.ms }

                mediaItemsCache[mediaItem.mediaId] = BookProgressWithDuration(
                    mediaId = mediaItem.mediaId,
                    bookProgressId = progress.bookProgressId,
                    bookId = book.bookId,
                    chapterId = chapter.chapterId,
                    bookTitle = book.title,
                    chapterTitle = chapter.title,
                    author = chapter.author ?: book.author,
                    series = book.series,
                    coverUri = chapter.coverUri ?: book.coverUri,
                    totalChapters = progress.totalChapters,
                    currentChapter = chapterIndex + 1,
                    chapterStart = chapter.start,
                    chapterProgress = ContentDuration.ZERO,
                    chapterDuration = chapter.duration,
                    bookDuration = book.duration,
                    bookChapterStart = ContentDuration.ms(bookChapterStartMs),
                )

                acc.add(mediaItem)
                acc
            }
        }
    }

    fun getMedia(mediaId: MediaId) = mediaItemsCache[mediaId]

    fun clear() = mediaItemsCache.clear()

    fun clearBooks(bookIds: List<BookId>) {
        mediaItemsCache.filterValues { bookIds.contains(it.bookId) }.map {
            mediaItemsCache.remove(it.key)
        }
    }
}
