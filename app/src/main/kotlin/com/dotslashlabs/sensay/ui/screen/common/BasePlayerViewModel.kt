package com.dotslashlabs.sensay.ui.screen.common

import android.content.ComponentName
import android.content.Context
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.dotslashlabs.sensay.common.PlaybackConnectionState
import com.dotslashlabs.sensay.common.SensayPlayer
import com.dotslashlabs.sensay.service.PlaybackService
import com.dotslashlabs.sensay.ui.screen.player.Media
import com.dotslashlabs.sensay.util.BUNDLE_KEY_BOOK_ID
import com.dotslashlabs.sensay.util.BUNDLE_KEY_CHAPTER_ID
import com.dotslashlabs.sensay.util.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

abstract class BasePlayerViewModel<S : MavericksState>(
    initialState: S
) : MavericksViewModel<S>(initialState) {

    protected var player: SensayPlayer? = null

    protected fun attach(
        context: Context,
        withPlayer: (
            error: Throwable?,
            playerEvents: Flow<PlaybackConnectionState>?,
            serviceEvents: Flow<PlayerState>?,
        ) -> Unit,
    ) {
        player?.release()
        player = null

        val f = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()

        viewModelScope.launch(Dispatchers.Main) {
            try {
                player = SensayPlayer(f.await())
                withPlayer(null, player?.playerEvents, player?.serviceEvents)
            } catch (e: Throwable) {
                withPlayer(e, null, null)
            }
        }
    }

    protected fun detach() {
        player?.release()
        player = null
    }

    fun prepareMediaItems(
        selectedMedia: Media,
        mediaList: List<Media>,
        mediaIds: List<String>,
        playerMediaIds: List<String>,
    ) {

        val playerMediaIdx = playerMediaIds.indexOf(selectedMedia.mediaId)
        val hasCurrentChapterEnded =
            (selectedMedia.chapterDuration.ms - selectedMedia.chapterProgress.ms).absoluteValue < 20

        val startPositionMs = if (hasCurrentChapterEnded) 0L else selectedMedia.chapterProgress.ms

        viewModelScope.launch {
            val player = this@BasePlayerViewModel.player ?: return@launch

            player.apply {
                if (playerMediaIdx != -1) {
                    // selectedMediaId already exists in the player's media items
                    seekTo(playerMediaIdx, startPositionMs)
                } else {
                    val chapterIdx = mediaIds.indexOf(selectedMedia.mediaId)

                    val mediaItems = mediaList.map { c ->
                        MediaItem.Builder()
                            .setMediaId(c.mediaId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setExtras(
                                        bundleOf(
                                            BUNDLE_KEY_BOOK_ID to c.bookId,
                                            BUNDLE_KEY_CHAPTER_ID to c.chapterId,
                                        )
                                    )
                                    .build()
                            )
                            .build()
                    }

                    setMediaItems(mediaItems, chapterIdx, startPositionMs)
                }

                prepare()
            }
        }
    }
}
