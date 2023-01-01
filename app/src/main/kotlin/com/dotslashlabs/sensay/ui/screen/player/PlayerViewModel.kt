package com.dotslashlabs.sensay.ui.screen.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.media3.session.SessionResult
import androidx.work.await
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.*
import com.dotslashlabs.sensay.ui.PlayerAppViewActions
import com.dotslashlabs.sensay.ui.PlayerAppViewState
import com.dotslashlabs.sensay.ui.screen.common.BasePlayerViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookConfig
import data.entity.Bookmark
import data.entity.BookmarkType
import data.entity.BookmarkWithChapter
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat
import okhttp3.internal.toHexString

typealias Media = BookProgressWithDuration

data class PlayerViewState(
    @PersistState val bookId: Long,

    val isLoading: Boolean = false,
    val error: String? = null,

    val sliderPosition: Float = 0F,

    val mediaIds: List<String> = emptyList(),
    val mediaList: List<Media> = emptyList(),
    val media: Media? = null,

    val bookConfig: Async<BookConfig> = Uninitialized,
    val bookmarks: Async<List<BookmarkWithChapter>> = Uninitialized,

    val isEqPanelVisible: Boolean = false,

    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
) : MavericksState {

    constructor(args: PlayerViewArgs) : this(bookId = args.bookId)

    companion object {

        fun getSliderPosition(positionMs: Long?, durationMs: Long?): Float =
            if ((positionMs ?: 0) > 0 && (durationMs ?: 0) > 0)
                positionMs!!.toFloat().div(durationMs!!)
            else 0F
    }

    val coverUri: Uri? = media?.coverUri
    val mediaId: MediaId? = media?.mediaId

    val connState = playbackConnectionState()
    val playerMediaId = connState?.playerState?.mediaId

    val playerMediaIds = connState?.playerMediaIds ?: emptyList()

    val playerMediaIdx: Int = media?.mediaId?.let { selectedMediaId ->
        if (playerMediaId == selectedMediaId) {
            mediaList.indexOfFirst { it.mediaId == selectedMediaId }
        } else -1
    } ?: -1

    val playerMedia: Media? = if (playerMediaIdx != -1) {
        mediaList[playerMediaIdx]
    } else null

    val isActiveMedia: Boolean = (playerMedia != null)

    private val isPlaying = (connState?.playerState?.isPlaying == true)
    val isPlayingMedia: Boolean = (isActiveMedia && isPlaying)

    val enableResetSelectedMediaId =
        (mediaIds.contains(playerMediaId) && playerMediaId != media?.mediaId)

    val progressPair: Pair<Long?, Long?> = media?.let {
        it.chapterProgress.ms to it.chapterDuration.ms
    } ?: (null to null)

    val hasPreviousChapter = (playerMediaIdx - 1 >= 0 && playerMediaIdx < mediaList.size)
    val hasNextChapter = (playerMediaIdx >= 0 && playerMediaIdx + 1 < mediaList.size)

    val isBookmarkEnabled =
        (isActiveMedia && ((progressPair.first ?: 0L) >= 0 && (progressPair.second ?: 0L) > 0))

    val isEqPanelEnabled = (bookConfig is Success)
}

interface PlayerActions {
    fun attachPlayer(context: Context): Unit?
    fun detachPlayer(): Unit?

    fun previousChapter(
        playerAppViewActions: PlayerAppViewActions,
        playerAppViewState: PlayerAppViewState,
    ): Unit?

    fun nextChapter(
        playerAppViewActions: PlayerAppViewActions,
        playerAppViewState: PlayerAppViewState,
    ): Unit?

    fun seekTo(
        playerAppViewActions: PlayerAppViewActions,
        fraction: Float,
        ofDurationMs: Long,
    ): Unit?

    fun seekToPosition(
        playerAppViewActions: PlayerAppViewActions,
        mediaId: String,
        positionMs: Long,
        durationMs: Long,
    ): Unit?

    fun play(playerAppViewActions: PlayerAppViewActions): Unit?

    fun setSelectedMediaId(mediaId: String)
    fun resetSelectedMediaId()

    suspend fun createBookmark()
    fun deleteBookmark(bookmark: Bookmark)

    fun toggleEqPanel(isVisible: Boolean)

    fun toggleVolumeBoost(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean)
    fun toggleBassBoost(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean)
    fun toggleReverb(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean)
    fun toggleSkipSilence(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean)
}

@SuppressLint("UnsafeOptInUsageError")
class PlayerViewModel @AssistedInject constructor(
    @Assisted state: PlayerViewState,
    private val store: SensayStore,
) : BasePlayerViewModel<PlayerViewState>(state), PlayerActions {

    private var job: Job? = null

    init {
        val bookId = state.bookId

        combine(
            // load one-time data
            store.bookWithChapters(bookId).take(1),
            store.bookProgress(bookId),
            ::Pair,
        ).setOnEach { (bookWithChapters, bookProgress) ->

            val progressMediaId =
                PlayerAppViewState.getMediaId(bookProgress.bookId, bookProgress.chapterId)

            if (media?.mediaId == progressMediaId) {
                return@setOnEach this
            }

            val (book, chapters) = bookWithChapters
            val mediaItems = Media.fromBookAndChapters(
                bookProgress,
                book,
                chapters,
            )

            val progressMedia = mediaItems.first { it.mediaId == progressMediaId }

            val progressSliderPosition = PlayerViewState.getSliderPosition(
                progressMedia.chapterProgress.ms,
                progressMedia.chapterDuration.ms,
            )

            copy(
                isLoading = false,
                mediaIds = mediaIds.ifEmpty { mediaItems.map { it.mediaId } },
                media = progressMedia,
                sliderPosition = progressSliderPosition,
                mediaList = mediaItems,
            )
        }

        store.bookmarksWithChapters(bookId)
            .execute(retainValue = PlayerViewState::bookmarks) { copy(bookmarks = it) }

        // bit of a workaround
        viewModelScope.launch(Dispatchers.IO) {
            store.ensureBookConfig(bookId)
        }

        store.bookConfig(bookId)
            .execute(retainValue = PlayerViewState::bookConfig) { copy(bookConfig = it) }

        onEach(
            PlayerViewState::isActiveMedia,
            PlayerViewState::media,
            PlayerViewState::connState,
        ) { isActiveMedia, media, connState ->
            if (!isActiveMedia || media == null || connState?.playerState?.position == null) {
                return@onEach
            }

            val mediaUpdates = media.copy(
                chapterProgress = ContentDuration.ms(connState.playerState.position),
            )

            setState { copy(media = mediaUpdates) }
        }

        onEach(PlayerViewState::progressPair) { (positionMs, durationMs) ->
            val sliderPosition = PlayerViewState.getSliderPosition(
                positionMs,
                durationMs,
            )

            setState { copy(sliderPosition = sliderPosition) }
        }
    }

    override fun onCleared() {
        super.onCleared()

        detachPlayer()
    }

    override fun attachPlayer(context: Context) {
        setState { copy(isLoading = true) }
        attach(context) { err, playerEvents, _ ->
            logcat { "attachPlayer: ${player?.hashCode()?.toHexString()}" }
            setState { copy(isLoading = false, error = err?.message) }

            job?.cancel()
            job = playerEvents
                ?.execute(retainValue = PlayerViewState::playbackConnectionState) { state ->
                    copy(playbackConnectionState = state)
                }

            onEach(
                PlayerViewState::mediaId,
                PlayerViewState::playerMediaId,
            ) { mediaId, playerMediaId ->
                if (mediaId != null && mediaId == playerMediaId) {
                    player?.startLiveTracker(viewModelScope)
                } else {
                    player?.stopLiveTracker()
                }
            }
        }
    }

    override fun detachPlayer() {
        logcat { "detachPlayer: ${player?.hashCode()?.toHexString()}" }
        setState { copy(isLoading = false, error = null) }
        detach()

        job?.cancel()
        job = null
    }

    override fun play(playerAppViewActions: PlayerAppViewActions) {
        prepareMediaItems(playerAppViewActions)
        playerAppViewActions.play()
    }

    override fun previousChapter(
        playerAppViewActions: PlayerAppViewActions,
        playerAppViewState: PlayerAppViewState,
    ) = withState { state ->
        if (!state.hasPreviousChapter) return@withState

        setChapter(
            playerAppViewActions,
            playerAppViewState,
            state.mediaIds[state.playerMediaIdx - 1],
        )
    }

    override fun nextChapter(
        playerAppViewActions: PlayerAppViewActions,
        playerAppViewState: PlayerAppViewState,
    ) = withState { state ->
        if (!state.hasNextChapter) return@withState

        setChapter(
            playerAppViewActions,
            playerAppViewState,
            state.mediaIds[state.playerMediaIdx + 1],
        )
    }

    private fun setChapter(
        playerAppViewActions: PlayerAppViewActions,
        playerAppViewState: PlayerAppViewState,
        mediaId: String,
    ) {
        setSelectedMediaId(mediaId)

        viewModelScope.launch {
            if (playerAppViewState.isPlaying) {
                playerAppViewActions.play()
            } else {
                prepareMediaItems(playerAppViewActions)
                playerAppViewActions.pause()
            }
        }
    }

    override fun seekTo(
        playerAppViewActions: PlayerAppViewActions,
        fraction: Float,
        ofDurationMs: Long,
    ) = withState { state ->
        if (!state.isPlayingMedia) return@withState
        val mediaItemIndex = state.playerMediaIds.indexOf(state.playerMediaId)
        if (mediaItemIndex == -1) return@withState

        setState { copy(sliderPosition = fraction) }

        val positionMs = (fraction * ofDurationMs).toLong()

        viewModelScope.launch {
            playerAppViewActions.seekTo(mediaItemIndex, positionMs)
        }
    }

    override fun seekToPosition(
        playerAppViewActions: PlayerAppViewActions,
        mediaId: String,
        positionMs: Long,
        durationMs: Long,
    ) = withState { state ->
        val mediaIdx = state.mediaList.indexOfFirst { it.mediaId == mediaId }
        if (mediaIdx == -1) return@withState

        val media = state.mediaList[mediaIdx]
            .copy(chapterProgress = ContentDuration.ms(positionMs))

        setState { copy(media = media) }
        prepareMediaItems(playerAppViewActions)
        seekTo(playerAppViewActions, (positionMs.toFloat() / maxOf(1L, durationMs)), durationMs)
    }

    override suspend fun createBookmark() {
        val state = awaitState()
        val playerMedia = state.playerMedia ?: return

        val (positionMs, durationMs) = state.progressPair
        val chapterPosition = positionMs ?: return
        val chapterDuration = durationMs ?: return

        store.createBookmark(
            Bookmark(
                bookmarkType = BookmarkType.USER,
                chapterId = playerMedia.chapterId,
                bookId = playerMedia.bookId,
                chapterPosition = ContentDuration.ms(chapterPosition),
                chapterDuration = ContentDuration.ms(chapterDuration),
                title = playerMedia.chapterTitle,
            )
        )
    }

    override fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            store.deleteBookmark(bookmark)
        }
    }

    private fun prepareMediaItems(playerAppViewActions: PlayerAppViewActions) = withState { state ->
        val media = state.media ?: return@withState

        playerAppViewActions.prepareMediaItems(
            media,
            state.mediaList,
            state.mediaIds,
            state.playerMediaIds,
        )
    }

    override fun setSelectedMediaId(mediaId: String) {
        setState {
            copy(media = mediaList.first { it.mediaId == mediaId })
        }
    }

    override fun resetSelectedMediaId() = withState { state ->
        if (!state.enableResetSelectedMediaId) return@withState

        state.mediaList.firstOrNull { it.mediaId == state.playerMediaId }?.let { selectedMedia ->
            setState {
                copy(media = selectedMedia)
            }
        }
    }

    override fun toggleEqPanel(isVisible: Boolean) {
        setState { copy(isEqPanelVisible = isVisible) }
    }

    override fun toggleVolumeBoost(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean) =
        withState { state ->
            viewModelScope.launch {
                val result = playerAppViewActions.sendCustomCommand(
                    AudioEffectCommands.VOLUME_BOOST.toCommand(),
                    bundleOf(AudioEffectCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
                )?.await()

                if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                    state.bookConfig()?.copy(isVolumeBoostEnabled = isEnabled)?.let {
                        withContext(Dispatchers.IO) {
                            store.updateBookConfig(it)
                        }
                    }
                }
            }
        }

    override fun toggleBassBoost(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean) =
        withState { state ->
            viewModelScope.launch {
                val result = playerAppViewActions.sendCustomCommand(
                    AudioEffectCommands.BASS_BOOST.toCommand(),
                    bundleOf(AudioEffectCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
                )?.await()

                if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                    state.bookConfig()?.copy(isBassBoostEnabled = isEnabled)?.let {
                        withContext(Dispatchers.IO) {
                            store.updateBookConfig(it)
                        }
                    }
                }
            }
        }

    override fun toggleReverb(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean) =
        withState { state ->
            viewModelScope.launch {
                val result = playerAppViewActions.sendCustomCommand(
                    AudioEffectCommands.REVERB.toCommand(),
                    bundleOf(AudioEffectCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
                )?.await()

                if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                    state.bookConfig()?.copy(isReverbEnabled = isEnabled)?.let {
                        withContext(Dispatchers.IO) {
                            store.updateBookConfig(it)
                        }
                    }
                }
            }
        }

    override fun toggleSkipSilence(playerAppViewActions: PlayerAppViewActions, isEnabled: Boolean) =
        withState { state ->
            viewModelScope.launch {
                val result = playerAppViewActions.sendCustomCommand(
                    ExtraSessionCommands.SKIP_SILENCE.toCommand(),
                    bundleOf(ExtraSessionCommands.CUSTOM_ACTION_ARG_ENABLED to isEnabled),
                )?.await()

                if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                    state.bookConfig()?.copy(isSkipSilenceEnabled = isEnabled)?.let {
                        withContext(Dispatchers.IO) {
                            store.updateBookConfig(it)
                        }
                    }
                }
            }
        }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<PlayerViewModel, PlayerViewState> {
        override fun create(state: PlayerViewState): PlayerViewModel
    }

    companion object : MavericksViewModelFactory<PlayerViewModel, PlayerViewState>
    by hiltMavericksViewModelFactory()
}

data class PlayerViewArgs(
    val bookId: Long,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
    )

    constructor(bundle: Bundle) : this(bundle.getLong("bookId"))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(bookId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PlayerViewArgs> {
        override fun createFromParcel(parcel: Parcel): PlayerViewArgs {
            return PlayerViewArgs(parcel)
        }

        override fun newArray(size: Int): Array<PlayerViewArgs?> {
            return arrayOfNulls(size)
        }
    }
}
