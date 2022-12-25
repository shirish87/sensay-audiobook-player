package com.dotslashlabs.sensay.ui.screen.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.BookProgressWithDuration
import com.dotslashlabs.sensay.common.PlaybackConnectionState
import com.dotslashlabs.sensay.ui.screen.common.BasePlayerViewModel
import com.dotslashlabs.sensay.util.BUNDLE_KEY_BOOK_ID
import com.dotslashlabs.sensay.util.BUNDLE_KEY_CHAPTER_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.Bookmark
import data.entity.BookmarkType
import data.entity.BookmarkWithChapter
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import logcat.logcat
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

typealias Media = BookProgressWithDuration

data class PlayerViewState(
    @PersistState val bookId: Long,

    val isLoading: Boolean = false,
    val error: String? = null,

    val sliderPosition: Float = 0F,

    val mediaIds: List<String> = emptyList(),
    val mediaList: List<Media> = emptyList(),
    val media: Media? = null,

    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,

    val bookmarks: Async<List<BookmarkWithChapter>> = Uninitialized,

    val isEqPanelVisible: Boolean = false,
    val isVolumeBoostEnabled: Boolean = false,
    val isVoiceBoostEnabled: Boolean = false,
) : MavericksState {

    constructor(args: PlayerViewArgs) : this(bookId = args.bookId)

    companion object {
        const val DURATION_ZERO: String = "00:00:00"

        fun getMediaId(bookId: Long, chapterId: Long) = "books/$bookId/chapters/$chapterId"

        fun getSliderPosition(positionMs: Long?, durationMs: Long?): Float =
            if ((positionMs ?: 0) > 0 && (durationMs ?: 0) > 0)
                positionMs!!.toFloat().div(durationMs!!)
            else 0F
    }

    val coverUri: Uri? = media?.coverUri

    val connState = playbackConnectionState()

    val isConnected = (connState?.isConnected == true)

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
    val isPlayingMedia: Boolean = (isActiveMedia && connState?.playerState?.isPlaying == true)

    val enableResetSelectedMediaId =
        (mediaIds.contains(playerMediaId) && playerMediaId != media?.mediaId)

    val progressPair: Pair<Long?, Long?> = media?.let {
        it.chapterProgress.ms to it.chapterDuration.ms
    } ?: (null to null)

    val hasPreviousChapter = (playerMediaIdx - 1 >= 0 && playerMediaIdx < mediaList.size)
    val hasNextChapter = (playerMediaIdx >= 0 && playerMediaIdx + 1 < mediaList.size)

    val isBookmarkEnabled =
        (isActiveMedia && ((progressPair.first ?: 0L) >= 0 && (progressPair.second ?: 0L) > 0))

    fun formatTime(value: Long?): String = when (value) {
        null -> ""
        0L -> DURATION_ZERO
        else -> ContentDuration.format(value.milliseconds) ?: DURATION_ZERO
    }
}

interface PlayerActions {
    fun attachPlayer(context: Context)
    fun detachPlayer()

    fun previousChapter(): Unit?
    fun nextChapter(): Unit?
    fun seekBack(): Unit?
    fun seekForward(): Unit?
    fun seekTo(fraction: Float, ofDurationMs: Long): Unit?
    fun seekToPosition(mediaId: String, positionMs: Long, durationMs: Long): Unit?
    fun pause(): Unit?
    fun play(): Unit?

    fun setSelectedMediaId(mediaId: String)
    fun resetSelectedMediaId()

    suspend fun createBookmark()
    fun deleteBookmark(bookmark: Bookmark)

    fun toggleEqPanel(isVisible: Boolean)

    fun toggleVolumeBoost(isEnabled: Boolean)
    fun toggleVoiceBoost(isEnabled: Boolean)
}

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
        ) { a, b -> a to b }.setOnEach { (bookWithChapters, bookProgress) ->

            val progressMediaId =
                PlayerViewState.getMediaId(bookProgress.bookId, bookProgress.chapterId)

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

        onEach(PlayerViewState::isPlayingMedia) { isPlaying ->
            player?.apply {
                if (isPlaying) {
                    startLiveTracker(viewModelScope)
                } else {
                    stopLiveTracker()
                }
            }
        }

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
        logcat { "attachPlayer" }

        setState { copy(isLoading = true) }
        attach(context) { _, playerEvents, _ ->
            setState { copy(isLoading = false) }

            job?.cancel()
            job = playerEvents
                ?.execute(retainValue = PlayerViewState::playbackConnectionState) {
                    copy(playbackConnectionState = it)
                }
        }
    }

    override fun detachPlayer() {
        logcat { "detachPlayer" }
        detach()

        job?.cancel()
        job = null
    }

    override fun previousChapter() = withState { state ->
        if (!state.hasPreviousChapter) return@withState

        setChapter(state.mediaIds[state.playerMediaIdx - 1])
    }

    override fun nextChapter() = withState { state ->
        if (!state.hasNextChapter) return@withState

        setChapter(state.mediaIds[state.playerMediaIdx + 1])
    }

    private fun setChapter(mediaId: String) {
        setSelectedMediaId(mediaId)

        viewModelScope.launch {
            if (player?.isPlaying == true) {
                play()
            } else {
                prepareMediaItems()
                player?.pause()
            }
        }
    }

    override fun seekBack() = player?.seekBack()
    override fun seekForward() = player?.seekForward()

    override fun seekTo(fraction: Float, ofDurationMs: Long) = withState { state ->
        if (!state.isPlayingMedia) return@withState
        val mediaItemIndex = state.playerMediaIds.indexOf(state.playerMediaId)
        if (mediaItemIndex == -1) return@withState

        setState { copy(sliderPosition = fraction) }

        val positionMs = (fraction * ofDurationMs).toLong()

        viewModelScope.launch {
            player?.apply {
                seekTo(mediaItemIndex, positionMs)
            }
        }
    }

    override fun seekToPosition(mediaId: String, positionMs: Long, durationMs: Long) =
        withState { state ->
            val mediaIdx = state.mediaList.indexOfFirst { it.mediaId == mediaId }
            if (mediaIdx == -1) return@withState

            val media = state.mediaList[mediaIdx]
                .copy(chapterProgress = ContentDuration.ms(positionMs))

            setState { copy(media = media) }
            prepareMediaItems()
            seekTo((positionMs.toFloat() / maxOf(1L, durationMs)), durationMs)
        }

    override fun pause() = player?.pause()

    override fun play() {
        player?.apply {
            prepareMediaItems()

            playWhenReady = true
            play()
        }
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

    private fun prepareMediaItems() = withState { state ->
        val selectedMedia = state.media ?: return@withState

        // already set
        // if (state.playerMediaId == selectedMedia.mediaId) return@withState

        val playerMediaIdx = state.playerMediaIds.indexOf(selectedMedia.mediaId)
        val hasCurrentChapterEnded =
            (selectedMedia.chapterDuration.ms - selectedMedia.chapterProgress.ms).absoluteValue < 20

        val startPositionMs = if (hasCurrentChapterEnded) 0L else selectedMedia.chapterProgress.ms
        val chapters = state.mediaList

        viewModelScope.launch {
            val player = this@PlayerViewModel.player ?: return@launch

            player.apply {
                if (playerMediaIdx != -1) {
                    // selectedMediaId already exists in the player's media items
                    seekTo(playerMediaIdx, startPositionMs)
                } else {
                    val chapterIdx = state.mediaIds.indexOf(selectedMedia.mediaId)

                    val mediaItems = chapters.map { c ->
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

    override fun toggleVolumeBoost(isEnabled: Boolean) {
        setState { copy(isVolumeBoostEnabled = isEnabled) }
    }

    override fun toggleVoiceBoost(isEnabled: Boolean) {
        setState { copy(isVoiceBoostEnabled = isEnabled) }
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
