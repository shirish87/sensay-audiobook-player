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
                    bookProgress.chapters.indexOfFirst { c -> c.chapterId == chapterId }
                val chapter = bookProgress.chapters[chapterIndex]

                mediaItemsCache[it.mediaId] = BookProgressWithDuration(
                    bookProgressId = progress.bookProgressId,
                    bookId = bookId,
                    chapterId = chapter.chapterId,
                    totalChapters = progress.totalChapters,
                    currentChapter = chapterIndex,
                    chapterStart = chapter.start,
                    chapterProgress = ContentDuration.ZERO,
                    chapterDuration = chapter.duration,
                    // bookProgress = chapterStart + chapterProgress
                    bookDuration = bookProgress.book.duration,
                )

                acc.add(mediaItem)
                acc
            }
        }
    }

    fun getMedia(mediaId: MediaId) = mediaItemsCache[mediaId]
    fun clear() = mediaItemsCache.clear()
}
