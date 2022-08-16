package com.dotslashlabs.sensay.service

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import data.BookCategory
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import data.util.ContentDuration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.future
import logcat.logcat
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


data class PlayerEventUpdates(
    val currentMediaItem: MediaItem,
    val bookProgress: BookProgressWithBookAndChapters,
    val currentPositionMs: Long,
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
                player.currentMediaItem!!,
                mediaItemsCache[mediaId]!!,
                player.currentPosition,
            )
        },
    )

    // mediaItem.mediaId => BookProgressWithBookAndChapters
    private val mediaItemsCache: MutableMap<String, BookProgressWithBookAndChapters> =
        mutableMapOf()

    init {
        serviceScope.launch {
            _playerEvents.filterNotNull().debounce(5.seconds).collectLatest {
                val bookProgress = it.bookProgress
                logcat { "PlayerEvent.updateBookProgress: ${bookProgress.book.title} ${it.currentPositionMs}" }

                store.updateBookProgress(
                    bookProgress.bookProgress.copy(
                        bookProgress = ContentDuration(it.currentPositionMs.milliseconds),
                        bookCategory = if (it.currentPositionMs < it.bookProgress.durationMs)
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
            )

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
            val mediaItem = bookProgress.toMediaItem(it.mediaId, chapterId) ?: return@fold acc

            acc.add(mediaItem)
            mediaItemsCache[it.mediaId] = bookProgress
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


fun BookProgressWithBookAndChapters.toMediaItem(mediaId: String, chapterId: Long?): MediaItem? {
    val requestedChapter = when (chapterId) {
        null -> null
        chapter.chapterId -> chapter
        else -> chapters.find { c -> chapterId == c.chapterId }
    }

    if (chapterId != null && (requestedChapter?.start == null || requestedChapter.end == null)) {
        // if a chapter is explicitly requested, that chapter must exist
        // and must have clear start and end bounds
        return null
    }

    val extras = if (requestedChapter != null) {
        bundleOf(
            PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
            PlaybackConnection.BUNDLE_KEY_CHAPTER_ID to requestedChapter.chapterId,
        )
    } else {
        bundleOf(
            PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
        )
    }

    return MediaItem.Builder()
        .setUri(book.uri)
        .setMediaId(mediaId)
        .apply {
            if (requestedChapter != null) {
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(requestedChapter.start!!.ms)
                        .setEndPositionMs(requestedChapter.end!!.ms)
                        .build()
                )
            }
        }
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(requestedChapter?.title ?: book.title)
                .setAlbumTitle(if (requestedChapter != null) book.title else null)
                .setArtist(book.author)
                .setIsPlayable(true)
                .setTrackNumber(chapters.indexOf(chapter) + 1)
                .setTotalTrackCount(chapters.size)
                .setExtras(extras)
                .build()
        )
        .build()
}
