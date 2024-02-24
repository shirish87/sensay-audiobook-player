package media

import android.net.Uri
import android.os.Parcelable
import androidx.media3.common.Player
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaPlayerFlags(
    val isSeekBackEnabled: Boolean = false,
    val isSeekToPreviousEnabled: Boolean = false,

    val isPlayPauseEnabled: Boolean = false,

    val isSeekToNextEnabled: Boolean = false,
    val isSeekForwardEnabled: Boolean = false,

    val isSliderEnabled: Boolean = false,
) : Parcelable

@Parcelize
sealed class MediaPlayerState(
    val isLoading: Boolean,
    val currentMediaId: String? = null,
    val currentMediaItemIndex: Int? = null,
    val position: Long? = null,
    val duration: Long? = null,
    val flags: MediaPlayerFlags = MediaPlayerFlags()
) : Parcelable {

    data class Idle(
        val loading: Boolean,
        val mediaId: String? = null,
        val mediaItemIndex: Int? = null,
        val mediaPosition: Long? = null,
        val mediaDuration: Long? = null,
        val mediaFlags: MediaPlayerFlags = MediaPlayerFlags(),
    ) : MediaPlayerState(loading, mediaId, mediaItemIndex, mediaPosition, mediaDuration, mediaFlags)

    data class Playing(
        val mediaId: String?,
        val mediaItemIndex: Int?,
        val mediaPosition: Long? = null,
        val mediaDuration: Long? = null,
        val mediaFlags: MediaPlayerFlags = MediaPlayerFlags(),
    ) : MediaPlayerState(false, mediaId, mediaItemIndex, mediaPosition, mediaDuration, mediaFlags)

    data class Error(
        val error: String,
        val mediaId: String? = null,
        val mediaItemIndex: Int? = null,
        val mediaPosition: Long? = null,
        val mediaDuration: Long? = null,
        val mediaFlags: MediaPlayerFlags = MediaPlayerFlags(),
    ) : MediaPlayerState(false, mediaId, mediaItemIndex, mediaPosition, mediaDuration, mediaFlags)

    @IgnoredOnParcel
    val isPlaying by lazy { this is Playing }

    @IgnoredOnParcel
    val isError by lazy { this is Error }

    @IgnoredOnParcel
    val isEnabled by lazy { !(isLoading || isError) }
}