package com.dotslashlabs.sensay.common

import android.annotation.SuppressLint
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
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
import logcat.LogPriority
import logcat.logcat

typealias MediaId = String

class MediaSessionQueue(private val store: SensayStore) {

    private val scope = MainScope() + CoroutineName(this::class.java.simpleName)

    private val mediaItemsCache: MutableMap<MediaId, BookProgressWithDuration> = mutableMapOf()

    private val appliedAudioEffects: MutableMap<AudioEffectCommands, AudioEffect> = mutableMapOf()

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

        // Configure commands available to the controller in onConnect()
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .apply {
                    ExtraSessionCommands.commands.forEach(::add)
                    AudioEffectCommands.commands.forEach(::add)
                }
                .build()

            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                connectionResult.availablePlayerCommands,
            )
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> = scope.future {

            (session.player as? ExoPlayer)?.apply {
                val sessionCommand = ExtraSessionCommands.resolve(customCommand.customAction)
                if (sessionCommand != null) {
                    skipSilenceEnabled = ExtraSessionCommands.isEnabled(args)
                    logcat { "Applied skipSilenceEnabled: $skipSilenceEnabled" }
                    return@future SessionResult(SessionResult.RESULT_SUCCESS)
                }

                val audioEffectCommand = AudioEffectCommands.resolve(customCommand.customAction)
                if (audioEffectCommand != null && audioSessionId > 0) {
                    try {
                        AudioEffectCommands.toAudioEffect(
                            audioSessionId,
                            customCommand,
                            isEnabled = AudioEffectCommands.isEnabled(args),
                        )?.apply {
                            setAuxEffectInfo(AuxEffectInfo(id, 1F))

                            if (enabled) {
                                appliedAudioEffects[audioEffectCommand] = this
                            } else {
                                appliedAudioEffects.remove(audioEffectCommand)?.apply {
                                    setEnableStatusListener(null)
                                    release()
                                }
                            }

                            logcat { "Applied effect: ${customCommand.customAction}=${enabled}" }
                            return@future SessionResult(SessionResult.RESULT_SUCCESS)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()

                        return@future SessionResult(
                            SessionResult.RESULT_ERROR_NOT_SUPPORTED,
                            bundleOf(AudioEffectCommands.RESULT_ARG_ERROR to e.message),
                        )
                    }
                }
            }

            return@future SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
        }
    }

    fun getMedia(mediaId: MediaId) = mediaItemsCache[mediaId]

    fun release() {
        try {
            appliedAudioEffects.values.forEach { it.release() }
            appliedAudioEffects.clear()
            logcat { "Released audio effects" }
        } catch (e: Throwable) {
            logcat(LogPriority.WARN) { "Error releasing audio effects: ${e.message}" }
        }

        mediaItemsCache.clear()
    }

    fun clearBooks(bookIds: List<BookId>) {
        mediaItemsCache.filterValues { bookIds.contains(it.bookId) }.map {
            mediaItemsCache.remove(it.key)
        }
    }
}
