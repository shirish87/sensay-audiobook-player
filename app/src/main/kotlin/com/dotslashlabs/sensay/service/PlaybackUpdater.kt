package com.dotslashlabs.sensay.service

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.dotslashlabs.sensay.util.*
import com.google.common.util.concurrent.ListenableFuture
import config.ConfigStore
import data.BookCategory
import data.SensayStore
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.future
import logcat.logcat
import java.time.Instant
import kotlin.time.Duration.Companion.seconds


class PlaybackUpdater constructor(
    private val store: SensayStore,
    private val configStore: ConfigStore,
) : Player.Listener,
    MediaSession.Callback {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _playerEvents: MutableStateFlow<PlayerState?> = MutableStateFlow(null)

    private var playerRef: Player? = null

    private val stateRecorder = PlayerStateRecorder(
        10.seconds,
        { playerRef },
        _playerEvents,
        { player ->
            val state = player.state
            val mediaId = state.mediaId ?: return@PlayerStateRecorder null
            if (!mediaItemsCache.containsKey(mediaId)) return@PlayerStateRecorder null

            state
        },
    )

    // mediaItem.mediaId => (BookProgress, bookDurationMs: Long)
    private val mediaItemsCache: MutableMap<String, Pair<BookProgress, Long>> = mutableMapOf()

    init {
        serviceScope.launch {
            _playerEvents.filterNotNull().debounce(5.seconds).collectLatest {
                val (bookProgress, bookDurationMs) = mediaItemsCache[it.mediaId]
                    ?: return@collectLatest

                val chapterProgressMs = (it.position ?: 0L)
                if (chapterProgressMs < 0) return@collectLatest

                val bookProgressMs = bookProgress.bookProgress.ms + chapterProgressMs
                if (bookProgressMs < 0 || bookProgressMs > bookDurationMs) return@collectLatest

                logcat { "PlayerEvent.updateBookProgress: ${it.mediaId}" }

                store.updateBookProgress(
                    bookProgress.copy(
                        chapterProgress = ContentDuration.ms(chapterProgressMs),
                        bookProgress = ContentDuration.ms(bookProgressMs),
                        bookCategory = when {
                            (bookProgressMs == 0L) -> BookCategory.NOT_STARTED
                            (bookProgressMs == bookDurationMs) -> BookCategory.FINISHED
                            else -> BookCategory.CURRENT
                        },
                        lastUpdatedAt = Instant.now(),
                    )
                )
            }
        }
    }

    fun configure(player: Player): PlaybackUpdater {
        mediaItemsCache.clear()

        playerRef = player.apply {
            addListener(this@PlaybackUpdater)
        }

        return this
    }

//    override fun onEvents(player: Player, events: Player.Events) = stateRecorder.recordState(player)

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState != Player.STATE_IDLE) {
            stateRecorder.recordState()
        }

        serviceScope.launch {
            configStore.setLastPlayedBookId(playerRef?.bookId)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) =
        stateRecorder.recordState()

    override fun onIsLoadingChanged(isLoading: Boolean) =
        stateRecorder.recordState()

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            stateRecorder.startStateRecorder(serviceScope)
        } else {
            stateRecorder.stopStateRecorder()
            stateRecorder.recordState()
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
            val bookId = it.bookId ?: return@mapNotNull null
            val chapterId = it.chapterId ?: return@mapNotNull null

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

            val progress = bookProgress.bookProgress
            val chapterIndex = bookProgress.chapters.indexOfFirst { c -> c.chapterId == chapterId }
            val chapter = bookProgress.chapters[chapterIndex]

            mediaItemsCache[it.mediaId] = BookProgress(
                bookProgressId = progress.bookProgressId,
                bookId = bookId,
                chapterId = chapter.chapterId,
                totalChapters = progress.totalChapters,
                currentChapter = chapterIndex,
                chapterProgress = ContentDuration.ZERO,
                // Set to start-of-chapter, so future bookProgress += currentPosition
                bookProgress = chapter.start,
            ) to bookProgress.book.duration.ms

            acc.add(mediaItem)
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
