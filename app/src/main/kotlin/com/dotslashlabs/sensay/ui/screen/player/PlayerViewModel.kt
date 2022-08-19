package com.dotslashlabs.sensay.ui.screen.player

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.service.PlaybackConnection
import com.dotslashlabs.sensay.service.PlaybackConnectionState
import com.dotslashlabs.sensay.util.BUNDLE_KEY_BOOK_ID
import com.dotslashlabs.sensay.util.BUNDLE_KEY_CHAPTER_ID
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.Book
import data.entity.BookProgress
import data.entity.Chapter
import data.util.ContentDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.logcat
import kotlin.time.Duration.Companion.milliseconds

data class PlayerViewState(
    @PersistState val bookId: Long,

    val mediaId: String? = null,
    val selectedMediaId: String? = null,
    val preparingMediaId: String? = null,

    val isLoading: Boolean = false,
    val error: String? = null,

    val sliderPosition: Float = 0F,
    val isSliderDisabled: Boolean = false,

    val book: Book? = null,
    val bookProgress: BookProgress? = null,

    val chapters: List<Chapter> = emptyList(),
    val mediaIds: List<String> = emptyList(),

    val playbackConnectionState: Async<PlaybackConnectionState> = Uninitialized,
) : MavericksState {

    constructor(args: PlayerViewArgs) : this(bookId = args.bookId)

    companion object {
        const val DURATION_ZERO: String = "00:00:00"

        fun getMediaId(bookId: Long, chapterId: Long) = "books/${bookId}/chapters/${chapterId}"

        fun getSliderPosition(positionMs: Long?, durationMs: Long?): Float =
            if ((positionMs ?: 0) > 0 && (durationMs ?: 0) > 0)
                positionMs!!.toFloat().div(durationMs!!)
            else 0F
    }

    val coverUri: Uri? = book?.coverUri

    private val connState = playbackConnectionState()

    val isConnected = (connState?.isConnected == true)

    private val isPlaying = (connState?.playerState?.isPlaying == true)

    val playerMediaId = connState?.playerState?.mediaId

    val playerMediaIds = connState?.playerMediaIds ?: emptyList()

    val isMediaIdPreparing = (preparingMediaId != null && preparingMediaId == mediaId)

    private val isMediaIdCurrent = (mediaId != null && mediaId == playerMediaId)
    val isMediaIdPlaying = (isMediaIdCurrent && isPlaying)

    val isSelectedMediaIdCurrent = (isMediaIdCurrent && mediaId == selectedMediaId)
    val isSelectedMediaIdPlaying = (isSelectedMediaIdCurrent && isPlaying)

//    val enableResetSelectedMediaId =
//        (playerMediaId != null && isMediaIdCurrent && mediaId != selectedMediaId)

    private val selectedChapterMediaIdx = selectedMediaId?.let { mediaIds.indexOf(it) } ?: -1

    val enableResetSelectedMediaId =
        (selectedChapterMediaIdx != -1 && bookProgress?.chapterId != null &&
                chapters[selectedChapterMediaIdx].chapterId != bookProgress.chapterId)

    // mediaId => Chapter
    val selectedChapter: Pair<String, Chapter>? = if (selectedChapterMediaIdx != -1)
        mediaIds[selectedChapterMediaIdx] to chapters[selectedChapterMediaIdx]
    else null

    private val isSelectedMediaIdWithProgress =
        (selectedChapter?.second?.chapterId == bookProgress?.chapterId)

    val progressPair: Pair<Long?, Long?> = if (isSelectedMediaIdCurrent) {
        (connState?.playerState?.position ?: 0L) to (connState?.playerState?.duration ?: 0L)
    } else if (isSelectedMediaIdWithProgress) {
        (bookProgress?.chapterProgress?.ms ?: 0L) to (selectedChapter?.second?.duration?.ms ?: 0L)
    } else if (selectedChapter != null) {
        0L to selectedChapter.second.duration.ms
    } else (null to null)

    fun formatTime(value: Long?): String = when (value) {
        null -> ""
        0L -> DURATION_ZERO
        else -> ContentDuration.format(value.milliseconds) ?: DURATION_ZERO
    }
}

interface PlayerActions {
    var playWhenReady: Boolean

    fun seekBack(): Unit?
    fun seekForward(): Unit?
    fun seekTo(fraction: Float, ofDurationMs: Long): Unit?
    fun pause(): Unit?
    fun play(): Unit?

    fun setSelectedMediaId(mediaId: String)
    fun resetSelectedMediaId()
}

class PlayerViewModel @AssistedInject constructor(
    @Assisted private val state: PlayerViewState,
    private val playbackConnection: PlaybackConnection,
    store: SensayStore,
) : MavericksViewModel<PlayerViewState>(state), PlayerActions {

    init {
        val bookId = state.bookId

        store.bookProgressWithBookAndChapters(bookId)
            .execute { res ->
                when (res) {
                    is Success -> {
                        val progress = res()

                        val mediaId = this.mediaId
                            ?: PlayerViewState.getMediaId(bookId, progress.chapter.chapterId)

                        var newState = copy(
                            bookProgress = progress.bookProgress,
                            mediaId = mediaId,
                            selectedMediaId = this.selectedMediaId ?: mediaId,
                        )

                        if (this.book == null) {
                            val chapters = progress.chapters.sortedBy { c -> c.trackId }

                            newState = newState.copy(
                                isLoading = false,
                                book = progress.book,
                                chapters = chapters,
                                mediaIds = chapters.map { c ->
                                    PlayerViewState.getMediaId(
                                        bookId,
                                        c.chapterId,
                                    )
                                },
                            )
                        }

                        newState
                    }
                    is Fail -> PlayerViewState(
                        isLoading = false,
                        bookId = bookId,
                        error = res.error.message,
                    )
                    is Loading -> PlayerViewState(
                        isLoading = true,
                        bookId = bookId,
                    )
                    else -> this
                }
            }

        playbackConnection.state
            .execute(retainValue = PlayerViewState::playbackConnectionState) {
                copy(playbackConnectionState = it)
            }

        onEach(PlayerViewState::isMediaIdPlaying) { isCurrentBookPlaying ->
            if (isCurrentBookPlaying) {
                playbackConnection.startLiveUpdates(viewModelScope)
            } else {
                playbackConnection.stopLiveUpdates()
            }
        }

        onEach(
            PlayerViewState::preparingMediaId,
            PlayerViewState::playerMediaId
        ) { preparingMediaId, playerMediaId ->
            if (preparingMediaId != null && preparingMediaId == playerMediaId) {
                logcat { "reset preparingMediaId" }
                setState { copy(preparingMediaId = null) }
            }
        }

        onEach(
            PlayerViewState::progressPair,
            PlayerViewState::selectedMediaId,
        ) { (position, duration), _ ->
            setState {
                copy(
                    sliderPosition = PlayerViewState.getSliderPosition(position, duration),
                    isSliderDisabled = false,
                )
            }
        }
    }

    val player: Player?
        get() = playbackConnection.player

    override var playWhenReady: Boolean
        get() = player?.playWhenReady == true
        set(value) {
            player?.playWhenReady = value
        }

    override fun seekBack() = player?.seekBack()
    override fun seekForward() = player?.seekForward()

    override fun seekTo(fraction: Float, ofDurationMs: Long) = withState { state ->
        if (!state.isSelectedMediaIdCurrent) return@withState
        val mediaItemIndex = state.playerMediaIds.indexOf(state.playerMediaId)
        if (mediaItemIndex == -1) return@withState

        setState { copy(sliderPosition = fraction, isSliderDisabled = true) }

        val positionMs = (fraction * ofDurationMs).toLong()

        viewModelScope.launch {
            player?.apply {
                seekTo(mediaItemIndex, positionMs)
            }

            delay(100.milliseconds)
            setState { copy(isSliderDisabled = false) }
        }
    }

    override fun pause() = player?.pause()

    override fun play() {
        prepareMediaItems()

        player?.apply {
            playWhenReady = true
            play()
        }
    }

    private fun prepareMediaItems() = withState { state ->
        val (selectedMediaId, selectedChapter) = state.selectedChapter ?: return@withState
        val bookProgress = state.bookProgress ?: return@withState

        // preparing items takes a while, possibly due to bundle stuff, so we disable playback
        setState { copy(mediaId = selectedMediaId, preparingMediaId = selectedMediaId) }

        // already set
        if (selectedMediaId == state.playerMediaId) return@withState

        val bookId = state.bookId
        val playerMediaIdx = state.playerMediaIds.indexOf(selectedMediaId)
        val mediaIds = state.mediaIds
        val chapters = state.chapters

        val startPositionMs = if (selectedChapter.chapterId == bookProgress.chapterId) {
            // selectedMediaId has existing progress recorded
            bookProgress.chapterProgress.ms
        } else 0L

        viewModelScope.launch {
            this@PlayerViewModel.player?.apply {
                if (playerMediaIdx != -1) {
                    // selectedMediaId already exists in the player's media items
                    seekTo(playerMediaIdx, startPositionMs)
                } else {
                    // player's media items need to reloaded
                    val chapterIdx = mediaIds.indexOf(selectedMediaId)
                    val mediaItems = chapters.map { c ->
                        val chapterId = c.chapterId

                        MediaItem.Builder()
                            .setMediaId(PlayerViewState.getMediaId(bookId, chapterId))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setExtras(
                                        bundleOf(
                                            BUNDLE_KEY_BOOK_ID to bookId,
                                            BUNDLE_KEY_CHAPTER_ID to chapterId,
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

    override fun setSelectedMediaId(mediaId: String) = withState {
        setState { copy(selectedMediaId = mediaId) }
    }

    override fun resetSelectedMediaId() {
        setState {
            bookProgress?.let {
                val mediaId = PlayerViewState.getMediaId(it.bookId, it.chapterId)
                copy(selectedMediaId = mediaId)
            } ?: this
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
