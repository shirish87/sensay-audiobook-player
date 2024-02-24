package media

import androidx.media3.common.MediaItem

sealed class MediaPlaylistState(
    val isLoading: Boolean,
    val mediaItems: List<MediaItem> = emptyList(),
) {

    data class Empty(val loading: Boolean) : MediaPlaylistState(loading)

    data class MediaItemsSet(val items: List<MediaItem>) : MediaPlaylistState(false, items)

    val isEmpty by lazy { (this is Empty) || mediaItems.isEmpty() }
}
