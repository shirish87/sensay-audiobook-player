package com.dotslashlabs.sensay.service

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.guava.future
import logcat.logcat


class PlaybackUpdater constructor(private val store: SensayStore) : Player.Listener,
    MediaSession.Callback {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var currentMediaItem: BookProgressWithBookAndChapters? = null
    private var playerRef: Player? = null

    // mediaItem.mediaId => BookProgressWithBookAndChapters
    private val mediaItemsCache: MutableMap<String, BookProgressWithBookAndChapters> =
        mutableMapOf()

    fun configure(player: Player) {
        mediaItemsCache.clear()

        playerRef = player.apply {
            addListener(this@PlaybackUpdater)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {

    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        registerCurrentMediatItem(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        logcat { "onIsPlayingChanged: ${currentMediaItem?.book?.title}" }
    }

    private fun registerCurrentMediatItem(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId ?: return

        if (mediaItemsCache.containsKey(mediaId)) {
            currentMediaItem = mediaItemsCache[mediaId]
            return
        }

        val bookId = PlaybackConnection.bookIdFromExtras(mediaItem.mediaMetadata.extras) ?: return
        serviceScope.launch {
            currentMediaItem = store.bookProgressWithBookAndChapters(bookId).firstOrNull()
        }
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> = serviceScope.future {

        mediaItemsCache.clear()

        val bookMediaMap = mediaItems.mapNotNull {
            val bookId = PlaybackConnection.bookIdFromExtras(it.mediaMetadata.extras)
                ?: return@mapNotNull null

            bookId to it.mediaId
        }.toMap()

        store.bookProgressWithBookAndChapters(bookMediaMap.keys).first().mapNotNull {
            val book = it.book
            val mediaId = bookMediaMap[book.bookId] ?: return@mapNotNull null
            mediaItemsCache[mediaId] = it

            MediaItem.Builder()
                .setUri(book.uri)
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(book.title)
                        .setArtist(book.author)
                        .setIsPlayable(true)
                        .setExtras(
                            bundleOf(
                                PlaybackConnection.BUNDLE_KEY_BOOK_ID to book.bookId,
                            )
                        )
                        .build()
                )
                .build()
        }.toMutableList()
    }

    fun release() {
        mediaItemsCache.clear()
        playerRef?.removeListener(this)
        playerRef = null
        serviceScope.cancel()
    }
}
