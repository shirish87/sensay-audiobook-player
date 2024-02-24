package com.dotslashlabs.sensay.ui.nowplaying

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_BACK
import androidx.media3.common.Player.COMMAND_SEEK_FORWARD
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.util.UnstableApi
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.ui.common.BasePlayerViewModel
import com.dotslashlabs.sensay.ui.screen.player.PlayerViewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.logcat
import media.BindConnection
import media.MediaPlayerFlags
import media.MediaPlayerState
import media.MediaPlaylistState
import media.service.MediaService
import media.service.MediaServiceState


data class NowPlayingViewState(
    val serviceState: Async<MediaServiceState> = Uninitialized,
    val playerState: Async<MediaPlayerState> = Uninitialized,
    val playlistState: Async<MediaPlaylistState> = Uninitialized,
    val isActive: Boolean = false,
) : MavericksState {

//    val isReady = (playerState is Success || playerState is Fail)

    val isPlayerAttached: Boolean = (playerState is Success)

//    private val isPlaylistAttached: Boolean = (playlistState is Success)

//    val isPlayerReady: Boolean = (isPlayerAttached && isPlaylistAttached)

//    private val playlistMediaItems = playlistState()?.mediaItems

//    private val playlistMediaIds by lazy {
//        playlistMediaItems?.map { it.mediaId } ?: emptyList()
//    }

//    private val currentMediaItemIndex = playerState()?.currentMediaItemIndex

//    val currentMediaItem: MediaItem? = currentMediaItemIndex?.let { mediaItemIndex ->
//        playlistMediaItems?.getOrNull(mediaItemIndex)
//    }

//    val isPlaylistEmpty: Boolean = (playlistMediaItems?.isEmpty() ?: true)

//    val hasNext: Boolean = isPlayerReady &&
//            currentMediaItemIndex?.let { mediaItemIndex ->
//                playlistMediaItems?.getOrNull(mediaItemIndex + 1) != null
//            } == true
//
//    val hasPrevious: Boolean = isPlayerReady &&
//            currentMediaItemIndex?.let { mediaItemIndex ->
//                playlistMediaItems?.getOrNull(mediaItemIndex - 1) != null
//            } == true

//    val sliderPosition: Float = if (playerState is Success) {
//        playerState()?.let {
//            if ((it.position ?: 0) > 0 && (it.duration ?: 0) > 0)
//                it.position!!.toFloat().div(it.duration!!)
//            else null
//        } ?: 0f
//    } else -1f

//    val isSliderEnabled: Boolean = (sliderPosition >= 0f)

    fun isCurrent(playerViewState: PlayerViewState): Boolean {
        val playerMediaId = playerState()?.currentMediaId
        val mediaId = playerViewState.visibleMediaItem?.mediaId
        return (playerMediaId != null && playerMediaId == mediaId)
    }
}


class NowPlayingViewModel @AssistedInject constructor(
    @Assisted initialState: NowPlayingViewState,
    bindConnection: BindConnection<MediaService>,
    mediaServiceComponentName: ComponentName,
    @ApplicationContext val context: Context,
) : BasePlayerViewModel<NowPlayingViewState>(
    initialState,
    bindConnection,
    mediaServiceComponentName,
    context,
) {

    @UnstableApi
    fun withVisibleMediaAsCurrent(
        viewState: PlayerViewState,
        withPlayer: (Player) -> Unit,
    ) = viewModelScope.launch {

        val state = awaitState()
        val playerState = viewState.visiblePlayerState() ?: return@launch

        if (state.isCurrent(viewState)) {
            player?.let { withPlayer(it) }
            return@launch
        }

        val mediaItems = viewState.contextPlaylistState()?.mediaItems ?: emptyList()
        if (mediaItems.isEmpty()) return@launch

        val startIndex = playerState.currentMediaItemIndex  ?: 0
        if (startIndex !in mediaItems.indices) return@launch

        val startPositionMs = playerState.position ?: 0

        player?.apply {
            clearMediaItems()
            setMediaItems(mediaItems, startIndex, startPositionMs)
            playWhenReady = false
            prepare()

            withPlayer(this)
        }
    }

    fun setActive(active: Boolean, progressUpdateInterval: Long = 2000L) {
        setState {
            copy(isActive = active)
        }

        withState {
            logcat { "updatePlayProgress(${it.isActive})" }
            updatePlayProgress(it.isActive, progressUpdateInterval)
        }
    }

    override suspend fun shouldUpdateProgress(): Boolean {
        return awaitState().isActive
    }

    override fun onMediaServiceConnect(): Job = viewModelScope.launch {
        setState {
            copy(
                serviceState = Loading(),
                playerState = Loading(),
                playlistState = Loading(),
            )
        }

        service?.serviceStateFlow
            ?.execute(retainValue = NowPlayingViewState::serviceState) {
                copy(serviceState = it)
            }
    }

    override fun onMediaServiceDisconnect() {
        setState {
            copy(
                serviceState = Uninitialized,
                playerState = Uninitialized,
                playlistState = Uninitialized,
            )
        }
    }

    override fun onMediaPlayerConnect() {
        player?.run {
            setState { copy(playerState = Loading(), playlistState = Loading()) }
            publishFullUpdate()
        }
    }

    override fun onMediaPlayerDisconnect() {
        setState { copy(playerState = Uninitialized, playlistState = Uninitialized) }
    }

    override fun publishPlayerUpdate() {
        player?.toPlayerState()?.let {
            setState { copy(playerState = Success(it)) }
        }
    }

    override fun publishFullUpdate() {
        player?.run {
            val playlistState = toPlaylistState()
            val playerState = toPlayerState()

            setState {
                copy(
                    playerState = Success(playerState),
                    playlistState = Success(playlistState),
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<NowPlayingViewModel, NowPlayingViewState> {
        override fun create(state: NowPlayingViewState): NowPlayingViewModel
    }

    companion object :
        MavericksViewModelFactory<NowPlayingViewModel, NowPlayingViewState> by hiltMavericksViewModelFactory()
}

fun Player.toPlayerState(): MediaPlayerState {
    val mediaIndex = if (currentMediaItemIndex == C.INDEX_UNSET) null else currentMediaItemIndex
    val progressDuration = if (duration == C.TIME_UNSET) null else duration.coerceAtLeast(0)

    val progressPosition = if (currentPosition == C.TIME_UNSET || progressDuration == null) null
    else currentPosition.coerceIn(0, progressDuration)

    val flags = MediaPlayerFlags(
        isSeekToPreviousEnabled = isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS),
        isSeekToNextEnabled = isCommandAvailable(COMMAND_SEEK_TO_NEXT),
        isSeekBackEnabled = isCommandAvailable(COMMAND_SEEK_BACK),
        isSeekForwardEnabled = isCommandAvailable(COMMAND_SEEK_FORWARD),
        isPlayPauseEnabled = isCommandAvailable(COMMAND_PLAY_PAUSE),
        isSliderEnabled = isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
    )

    playerError?.let { error ->
        return MediaPlayerState.Error(
            error.message ?: "Unknown error",
            currentMediaItem?.mediaId,
            mediaIndex,
            progressPosition,
            progressDuration,
            flags,
        )
    }

    return if (isPlaying) {
        MediaPlayerState.Playing(
            currentMediaItem?.mediaId,
            mediaIndex,
            progressPosition,
            progressDuration,
            flags,
        )
    } else {
        MediaPlayerState.Idle(
            isLoading,
            currentMediaItem?.mediaId,
            mediaIndex,
            progressPosition,
            progressDuration,
            flags,
        )
    }
}

fun Player.toPlaylistState(): MediaPlaylistState {
    return if (currentMediaItemIndex == C.INDEX_UNSET || mediaItemCount == 0) {
        MediaPlaylistState.Empty(false)
    } else {
        MediaPlaylistState.MediaItemsSet(
            (currentMediaItemIndex until mediaItemCount).map(::getMediaItemAt),
        )
    }
}
