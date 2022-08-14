package com.dotslashlabs.sensay.service

import androidx.core.os.bundleOf
import androidx.media3.common.C
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
    private var stateRecorderJob: Job? = null

    // mediaItem.mediaId => BookProgressWithBookAndChapters
    private val mediaItemsCache: MutableMap<String, BookProgressWithBookAndChapters> =
        mutableMapOf()

    init {
        serviceScope.launch {
            _playerEvents.filterNotNull().debounce(5.seconds).collectLatest {
                val bookProgress = it.bookProgress
                logcat { "PlayerEvent: ${bookProgress.book.title} ${it.currentPositionMs}" }

                store.updateBookProgress(
                    bookProgress.bookProgress.copy(
                        bookProgress = ContentDuration(it.currentPositionMs.milliseconds),
                        bookCategory = if (it.currentPositionMs < it.bookProgress.bookDurationMs)
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

    override fun onEvents(player: Player, events: Player.Events) = recordState(player)

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        playerRef?.let { recordState(it) }
    }

    private fun recordState(player: Player) {
        if (player.currentPosition == C.TIME_UNSET) return
        val currentMediaItem = player.currentMediaItem ?: return
        if (!mediaItemsCache.containsKey(currentMediaItem.mediaId)) return

        _playerEvents.value = PlayerEventUpdates(
            player.currentMediaItem!!,
            mediaItemsCache[currentMediaItem.mediaId]!!,
            player.currentPosition,
        )
    }

    private fun startStateRecorder() {
        stateRecorderJob?.cancel()

        stateRecorderJob = serviceScope.launch {
            while (playerRef != null) {
                recordState(playerRef!!)
                delay(10.seconds.inWholeMilliseconds)
            }
        }
    }

    private fun stopStateRecorder() {
        stateRecorderJob?.cancel()
        stateRecorderJob = null
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            startStateRecorder()
        } else {
            stopStateRecorder()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future {

        playerRef?.clearMediaItems()
        mediaItemsCache.clear()

        val bookMediaMap = mediaItems.mapNotNull {
            val bookId = PlaybackConnection.bookIdFromExtras(it.mediaMetadata.extras)
                ?: return@mapNotNull null

            bookId to it.mediaId
        }.toMap()

        val result = mutableListOf<MediaItem>()

        store.bookProgressWithBookAndChapters(bookMediaMap.keys)
            .firstOrNull()
            ?.fold(result) { list, it ->
                val book = it.book
                val mediaId = bookMediaMap[book.bookId] ?: return@fold list
                mediaItemsCache[mediaId] = it

                list.add(it.toMediaItem(mediaId))
                list
            } ?: result
    }

    fun release() {
        stopStateRecorder()
        mediaItemsCache.clear()
        playerRef?.removeListener(this)
        playerRef = null
        serviceScope.cancel()
    }
}


fun BookProgressWithBookAndChapters.toMediaItem(mediaId: String) =
    MediaItem.Builder()
        .setUri(book.uri)
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(book.title)
                .setArtist(book.author)
                .setIsPlayable(true)
                .setTrackNumber(chapters.indexOf(chapter) + 1)
                .setTotalTrackCount(chapters.size)
                .setExtras(
                    bundleOf(
                        PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
                    )
                )
                .build()
        )
        .build()
