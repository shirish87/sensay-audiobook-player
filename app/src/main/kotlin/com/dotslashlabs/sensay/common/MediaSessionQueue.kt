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
import data.entity.ChapterId
import data.util.ContentDuration
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import logcat.logcat

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

            val mediaBookChapterMap: Map<MediaId, Pair<BookId, ChapterId>> = mediaItems.mapNotNull {
                val bookId = it.bookId ?: return@mapNotNull null
                val chapterId = it.chapterId ?: return@mapNotNull null

                it.mediaId to (bookId to chapterId)
            }.toMap()

            val bookProgressMap: Map<BookId, BookProgressWithBookAndChapters> =
                store.bookProgressWithBookAndChapters(mediaBookChapterMap.values.map { it.first })
                    .first()
                    .fold(mutableMapOf()) { acc, it ->
                        acc[it.book.bookId] = it
                        acc
                    }

            mediaItems.fold(mutableListOf()) { acc, it ->
                val (bookId, chapterId) = mediaBookChapterMap[it.mediaId] ?: return@fold acc
                val bookProgress = bookProgressMap[bookId] ?: return@fold acc
                val mediaItem = bookProgress.toMediaItem(it, chapterId) ?: return@fold acc

                val progress = bookProgress.bookProgress
                val chapterIndex =
                    bookProgress.chapters
                        .sortedBy { c -> c.trackId }
                        .indexOfFirst { c -> c.chapterId == chapterId }
                val chapter = bookProgress.chapters[chapterIndex]
                val book = bookProgress.book

                mediaItemsCache[it.mediaId] = BookProgressWithDuration(
                    mediaId = it.mediaId,
                    bookProgressId = progress.bookProgressId,
                    bookId = bookId,
                    chapterId = chapter.chapterId,
                    bookTitle = book.title,
                    chapterTitle = chapter.title,
                    author = chapter.author ?: book.author,
                    series = book.series,
                    coverUri = book.coverUri,
                    totalChapters = progress.totalChapters,
                    currentChapter = chapterIndex + 1,
                    chapterStart = chapter.start,
                    chapterProgress = ContentDuration.ZERO,
                    chapterDuration = chapter.duration,
                    // bookProgress = chapterStart + chapterProgress
                    bookDuration = book.duration,
                )

                logcat { "Queued media item: mediaId=${it.mediaId}" }
                acc.add(mediaItem)
                acc
            }
        }
    }

    fun getMedia(mediaId: MediaId) = mediaItemsCache[mediaId]
    fun clear() = mediaItemsCache.clear()
}
