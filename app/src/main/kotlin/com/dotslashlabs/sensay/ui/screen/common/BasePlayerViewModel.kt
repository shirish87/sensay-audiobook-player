package com.dotslashlabs.sensay.ui.screen.common

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.dotslashlabs.sensay.common.PlaybackConnectionState
import com.dotslashlabs.sensay.common.SensayPlayer
import com.dotslashlabs.sensay.service.PlaybackService
import com.dotslashlabs.sensay.util.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

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
}
