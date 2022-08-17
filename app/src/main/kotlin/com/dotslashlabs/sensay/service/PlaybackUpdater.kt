package com.dotslashlabs.sensay.service

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import data.BookCategory
import data.SensayStore
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.Chapter
import data.util.ContentDuration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.future
import logcat.logcat
import java.time.Instant
import kotlin.time.Duration.Companion.seconds


data class PlayerEventUpdates(
    val currentMediaItemIndex: Int,
    val currentMediaItem: MediaItem,
    val currentPositionMs: Long,
    val mediaData: MediaData,
)

data class MediaData(
    val bookId: Long,
    val chapterIndex: Int,
    val chapter: Chapter,
    val bookDurationMs: Long,
    val bookProgress: BookProgress,
)

class PlaybackUpdater constructor(private val store: SensayStore) : Player.Listener,
    MediaSession.Callback {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _playerEvents: MutableStateFlow<PlayerEventUpdates?> = MutableStateFlow(null)

    private var playerRef: Player? = null

    private val stateRecorder = PlayerStateRecorder(
        10.seconds,
        { playerRef },
        _playerEvents,
        { player ->
            val mediaId = player.currentMediaItem?.mediaId ?: return@PlayerStateRecorder null
            if (!mediaItemsCache.containsKey(mediaId)) return@PlayerStateRecorder null

            PlayerEventUpdates(
                player.currentMediaItemIndex,
                player.currentMediaItem!!,
                player.currentPosition,
                mediaItemsCache[mediaId]!!,
            )
        },
    )

    // mediaItem.mediaId => BookProgressWithBookAndChapters
    private val mediaItemsCache: MutableMap<String, MediaData> = mutableMapOf()

    init {
        serviceScope.launch {
            _playerEvents.filterNotNull().debounce(5.seconds).collectLatest {
                val progress = it.mediaData.bookProgress
                val chapterIndex = it.mediaData.chapterIndex
                val chapter = it.mediaData.chapter
                val bookProgressMs = chapter.start.ms + it.currentPositionMs

                logcat { "PlayerEvent.updateBookProgress: ${chapter.title} ${it.currentPositionMs}" }

                store.updateBookProgress(
                    progress.copy(
                        chapterId = chapter.chapterId,
                        currentChapter = chapterIndex,
                        chapterProgress = ContentDuration.ms(it.currentPositionMs),
                        bookProgress = ContentDuration.ms(bookProgressMs),
                        bookCategory = if (bookProgressMs < it.mediaData.bookDurationMs)
                            BookCategory.CURRENT
                        else BookCategory.FINISHED,
                        lastUpdatedAt = Instant.now(),
                    )
                )
            }
        }
    }

    fun configure(player: Player) {
        mediaItemsCache.clear()

        playerRef = player.apply {
            addListener(this@PlaybackUpdater)
        }
    }

    override fun onEvents(player: Player, events: Player.Events) = stateRecorder.recordState(player)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
        stateRecorder.recordState()

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            stateRecorder.startStateRecorder(serviceScope)
        } else {
            stateRecorder.stopStateRecorder()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future {

        playerRef?.clearMediaItems()
        mediaItemsCache.clear()

        // mediaItem.mediaId => (bookId, chapterId)
        val mediaBookChapterMap = mediaItems.mapNotNull {
            val bookId = PlaybackConnection.fromExtras(
                it.mediaMetadata.extras,
                PlaybackConnection.BUNDLE_KEY_BOOK_ID,
            ) ?: return@mapNotNull null

            val chapterId = PlaybackConnection.fromExtras(
                it.mediaMetadata.extras,
                PlaybackConnection.BUNDLE_KEY_CHAPTER_ID,
            ) ?: return@mapNotNull null

            it.mediaId to (bookId to chapterId)
        }.toMap()

        // bookId => BookProgressWithBookAndChapters
        val bookProgressMap: Map<Long, BookProgressWithBookAndChapters> =
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

            val chapterIndex = bookProgress.chapters.indexOfFirst { c -> c.chapterId == chapterId }
            val chapter = bookProgress.chapters[chapterIndex]

            acc.add(mediaItem)
            mediaItemsCache[it.mediaId] = MediaData(
                bookId,
                chapterIndex,
                chapter,
                bookProgress.durationMs,
                bookProgress.bookProgress,
            )

            acc
        }
    }

    fun release() {
        stateRecorder.stopStateRecorder()
        mediaItemsCache.clear()
        playerRef?.removeListener(this)
        playerRef = null

        if (serviceScope.isActive) {
            serviceScope.cancel()
        }
    }
}


fun BookProgressWithBookAndChapters.toMediaItem(mediaItem: MediaItem, chapterId: Long): MediaItem? {
    val resolvedChapter = when (chapterId) {
        chapter.chapterId -> chapter
        else -> chapters.find { c -> chapterId == c.chapterId }
    } ?: chapter

    if (resolvedChapter.isInvalid()) {
        return null
    }

    return mediaItem.buildUpon()
        .setUri(chapter.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(resolvedChapter.start.ms)
                .setEndPositionMs(resolvedChapter.end.ms)
                .build()
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("${book.title}: ${resolvedChapter.title}")
                .setArtist(book.author)
                .setIsPlayable(true)
                .setTrackNumber(chapters.indexOf(chapter) + 1)
                .setTotalTrackCount(chapters.size)
                .setExtras(
                    bundleOf(
                        PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
                        PlaybackConnection.BUNDLE_KEY_CHAPTER_ID to resolvedChapter.chapterId,
                    )
                )
                .build()
        )
        .build()
}
