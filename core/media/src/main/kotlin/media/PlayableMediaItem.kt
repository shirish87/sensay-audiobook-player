package media

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.dotslashlabs.media3.extractor.m4b.metadata.ChapterMetadata
import com.dotslashlabs.media3.extractor.m4b.metadata.M4bMetadata
import data.SensayStore
import kotlinx.parcelize.Parcelize
import java.util.UUID
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
data class ChapterMarker(
    val mediaId: String,
    val mediaUri: Uri,
    val index: Int,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val coverUri: Uri? = null,
) : Parcelable

fun <T> buildChapters(
    duration: Long?,
    chapterList: List<Triple<Uri, ChapterMetadata, M4bMetadata?>>,
    onChapter: (Int, Uri, ChapterMetadata, Long, Long, Long) -> T,
): List<T> {

    if (chapterList.isEmpty() || duration == null || duration == 0L) return emptyList()

    val lastElem = Triple(Uri.EMPTY, ChapterMetadata((duration * 1e+4).roundToLong(), ""), null)

    return (chapterList + lastElem)
        .zipWithNext()
        .mapIndexedNotNull { index, (chapter, nextChapter) ->
            val startTime = chapter.second.startTimeMs
                .let { startTime ->
                    if (index == 0) startTime
                    else startTime + 1
                }

            val endTime = nextChapter.second.startTimeMs
            val chapterDuration = endTime - startTime
            if (chapterDuration <= 0) return@mapIndexedNotNull null

            return@mapIndexedNotNull onChapter(index, chapter.first, chapter.second, startTime, endTime, chapterDuration)
        }
}

fun buildChapterMarkers(
    duration: Long?,
    chapterList: List<Triple<String, Uri, ChapterMetadata>>,
): List<ChapterMarker> {

    if (chapterList.isEmpty() || duration == null || duration == 0L) return emptyList()

    return buildChapters(
        duration,
        chapterList.map { Triple(it.second, it.third, null) }) { index, _, chapter, startTime, endTime, chapterDuration ->

        ChapterMarker(
            chapterList[index].first,
            chapterList[index].second,
            index,
            chapter.chapterTitle,
            startTime,
            endTime,
            chapterDuration,
        )
    }
}

@Parcelize
data class PlayableMediaItem(
    private val bookId: Long,
    private val metadata: Bundle,
    private val chapterList: List<Pair<Uri, ChapterMetadata>>,

    private val duration: Long? = null,
    private val position: Long? = null,

    private val chapterIndex: Int = 0,
    private val chapterPosition: Long = 0L,

    val chapterMarkers: List<ChapterMarker> = buildChapterMarkers(
        duration,
        chapterList.mapIndexed { index, c ->
            Triple(
                SensayStore.mediaId(bookId, index),
                c.first,
                c.second,
            )
        },
    ),
) : Parcelable {

    companion object {
        const val MEDIA_ITEM_KEY_BOOK_ID = "bookId"
        const val MEDIA_ITEM_KEY_CHAPTER_INDEX = "chapterIndex"
        const val MEDIA_ITEM_KEY_CHAPTER_DURATION = "chapterDuration"
    }

    @get:UnstableApi
    private val mediaMetadata: MediaMetadata
        get() = MediaMetadata.fromBundle(metadata)

    val chapters: List<MediaItem>
        get() {
            val metadata = mediaMetadata.buildUpon()
                .setIsPlayable(true)
                .setArtist(mediaMetadata.artist)
                .setAlbumTitle(mediaMetadata.title)
                .setTotalTrackCount(totalTrackCount())
                .setArtworkData(mediaMetadata.artworkData, mediaMetadata.artworkDataType)
                .build()

            return chapterMarkers.mapIndexed { index, c ->
                MediaItem.Builder()
                    .setMediaId(c.mediaId)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(c.startTime)
                            .setEndPositionMs(c.endTime)
                            .build()
                    )
                    .setRequestMetadata(
                        MediaItem.RequestMetadata.Builder()
                            .setMediaUri(c.mediaUri)
                            .build()
                    )
                    .setMediaMetadata(
                        metadata.buildUpon()
                            .setTitle(c.title)
                            .setTrackNumber(index)
                            .setExtras(bundleOf(
                                MEDIA_ITEM_KEY_BOOK_ID to bookId,
                                MEDIA_ITEM_KEY_CHAPTER_INDEX to index,
                                MEDIA_ITEM_KEY_CHAPTER_DURATION to c.duration,
                            ))
                            .build()
                    )
                    .build()
            }
        }

    val chapter: MediaItem?
        get() = if (chapterIndex in chapters.indices)
            chapters[chapterIndex]
        else chapters.firstOrNull()

    val hash: String
        get() = UUID.nameUUIDFromBytes(
            """
              |${mediaMetadata.artist}
              |${mediaMetadata.albumArtist}
              |${mediaMetadata.title}
              |${mediaMetadata.albumTitle}
              |$duration
              |${chapters.fold(0L.milliseconds) { acc, c -> acc + c.chapterDuration.milliseconds }}
              |${chapters.size}
            """.trimMargin().trimIndent().encodeToByteArray()
        ).toString()

    fun mediaId() = SensayStore.mediaId(bookId, chapterIndex)

    fun totalTrackCount() = chapterMarkers.size

    fun toPlayerState() = MediaPlayerState.Idle(
        false,
        mediaId(),
        chapterIndex,
        position,
        duration,
    )

    fun toPlaylistState() = MediaPlaylistState.MediaItemsSet(
        chapters,
    )
}

val MediaItem.bookId
    get() = mediaMetadata.extras!!.getLong(PlayableMediaItem.MEDIA_ITEM_KEY_BOOK_ID)

val MediaItem.chapterIndex
    get() = mediaMetadata.extras!!.getInt(PlayableMediaItem.MEDIA_ITEM_KEY_CHAPTER_INDEX)

val MediaItem.chapterDuration
    get() = mediaMetadata.extras!!.getLong(PlayableMediaItem.MEDIA_ITEM_KEY_CHAPTER_DURATION)
