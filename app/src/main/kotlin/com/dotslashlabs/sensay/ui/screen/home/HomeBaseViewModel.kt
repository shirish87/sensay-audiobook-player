package com.dotslashlabs.sensay.ui.screen.home

import android.content.Context
import com.airbnb.mvrx.MavericksState
import com.dotslashlabs.sensay.ui.screen.common.BasePlayerViewModel
import com.dotslashlabs.sensay.ui.screen.player.Media
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewState
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty1

abstract class HomeBaseViewModel<S : MavericksState>(
    initialState: S
) : BasePlayerViewModel<S>(initialState) {

    fun attachPlayer(context: Context, onError: (e: Throwable?) -> Unit) =
        attach(context) { err, _, _ ->
            onError(err)
        }

    fun detachPlayer() = detach()

    fun play(bookProgressWithChapters: BookProgressWithBookAndChapters) {
        val player = player ?: return

        val (bookProgress, book, _, chapters) = bookProgressWithChapters
        val selectedMediaId =
            PlayerViewState.getMediaId(bookProgress.bookId, bookProgress.chapterId)

        val mediaList = Media.fromBookAndChapters(
            bookProgress,
            book,
            chapters,
        )

        val selectedMedia = mediaList.first { it.mediaId == selectedMediaId }
        val mediaIds = mediaList.map { it.mediaId }

        viewModelScope.launch(Dispatchers.Main) {
            val connState = player.playerEvents.firstOrNull()
            val isPlaying = (connState?.playerState?.isPlaying == true)

            if (connState?.playerState?.mediaId == selectedMediaId) {
                if (!isPlaying) {
                    player.play()
                }

                return@launch
            }

            val playerMediaIds = connState?.playerMediaIds ?: emptyList()

            prepareMediaItems(
                selectedMedia,
                mediaList,
                mediaIds,
                playerMediaIds,
            )

            if (!isPlaying) {
                player.play()
            }
        }
    }

    protected fun <A, B, C> onEachThrottled(
        prop1: KProperty1<S, A>,
        prop2: KProperty1<S, B>,
        prop3: KProperty1<S, C>,
        delayByMillis: (A, B, C) -> Long,
        action: suspend (A, B, C) -> Unit,
    ) {

        var loadingJob: Job? = null

        onEach(
            prop1,
            prop2,
            prop3,
        ) { val1, val2, val3 ->

            loadingJob?.cancel()
            loadingJob = viewModelScope.launch {
                delay(delayByMillis(val1, val2, val3))
                action(val1, val2, val3)
            }
        }
    }
}
