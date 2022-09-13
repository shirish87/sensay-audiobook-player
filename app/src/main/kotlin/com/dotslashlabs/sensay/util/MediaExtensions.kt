package com.dotslashlabs.sensay.util

import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import data.entity.BookProgressWithBookAndChapters

const val BUNDLE_KEY_BOOK_ID = "bookId"
const val BUNDLE_KEY_CHAPTER_ID = "chapterId"

val Player?.mediaId: String?
    get() = this?.currentMediaItem?.mediaId

val Player?.bookId: Long?
    get() = this?.currentMediaItem?.bookId

val Player?.chapterId: Long?
    get() = this?.currentMediaItem?.chapterId

val MediaItem?.bookId: Long?
    get() = this?.mediaMetadata?.bookId

val MediaItem?.chapterId: Long?
    get() = this?.mediaMetadata?.chapterId

val MediaMetadata?.bookId: Long?
    get() = this?.extras?.getLong(BUNDLE_KEY_BOOK_ID)

val MediaMetadata?.chapterId: Long?
    get() = this?.extras?.getLong(BUNDLE_KEY_CHAPTER_ID)

fun BookProgressWithBookAndChapters.toExtras(chapterId: Long? = null) = bundleOf(
    BUNDLE_KEY_BOOK_ID to book.bookId,
    BUNDLE_KEY_CHAPTER_ID to (chapterId ?: chapter.chapterId),
)

fun BookProgressWithBookAndChapters.toMediaItem(mediaItem: MediaItem, chapterId: Long): MediaItem? {
    val resolvedChapter = when (chapterId) {
        chapter.chapterId -> chapter
        else -> chapters.find { c -> chapterId == c.chapterId }
    } ?: return null

    if (resolvedChapter.isInvalid()) {
        return null
    }

    return mediaItem.buildUpon()
        .setUri(resolvedChapter.uri)
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
                .setTrackNumber(resolvedChapter.trackId)
                .setTotalTrackCount(chapters.size)
                .build()
        )
        .build()
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,

    val mediaId: String? = null,
    val position: Long? = null,
    val duration: Long? = null,
)

val Player?.state: PlayerState
    get() = PlayerState(
        isPlaying = (this?.isPlaying == true),
        isLoading = (this?.isLoading == true),

        mediaId = this?.mediaId,
        position = this?.currentPosition,
        duration = this?.duration,
    )

val Player?.mediaIds: List<String>
    get() = this?.let { p ->
        (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaId }
    } ?: emptyList()
