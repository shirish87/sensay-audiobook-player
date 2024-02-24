package scanner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.lang.Double.max
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MetaDataScanResult(
    val streams: List<MetaDataStream>,
    val chapters: List<MetaDataChapter>,
    val format: MetaDataFormat?
)

enum class TagType {
    Title, Artist, AlbumArtist, Album
}

fun MetaDataScanResult.findTag(tagType: TagType): String? {
    format?.tags?.find(tagType)?.let { return it }
    streams.forEach { stream ->
        stream.tags?.find(tagType)?.let { return it }
    }
    chapters.forEach { chapter ->
        chapter.tags?.find(tagType)?.let { return it }
    }
    return null
}

fun getTagKey(tagType: TagType): String = when (tagType) {
    TagType.Title -> "title"
    TagType.Artist -> "artist"
    TagType.AlbumArtist -> "album_artist"
    TagType.Album -> "album"
}

fun Map<String, String>.find(tagType: TagType): String? {
    val targetKey = getTagKey(tagType)

    forEach { (key, value) ->
        if (key.replace("_", "").equals(targetKey, ignoreCase = true) && value.isNotEmpty()) {
            return value
        }
    }
    return null
}

@Serializable
data class MetaDataStream(
    val tags: Map<String, String>? = null
)

@Serializable
data class MetaDataChapter(
    @SerialName("id") val id: Int,
    @SerialName("start_time") private val startTime: Double,
    @SerialName("end_time") private val endTime: Double,
    val tags: Map<String, String>? = null
) {

    val start: Duration get() = startTime.seconds
    val end: Duration get() = endTime.seconds

    val artist by lazy { tags?.find(TagType.Artist) }
    val albumArtist by lazy { tags?.find(TagType.AlbumArtist) }
    val album by lazy { tags?.find(TagType.Album) }

    val title by lazy {
        tags?.find(TagType.Title) ?: (id + 1).toString()
    }

    val titleTag: String? by lazy {
        tags?.keys?.find { it.equals(getTagKey(TagType.Title), true) }
    }

    val duration: Duration by lazy {
        max(0.0, endTime - startTime).seconds
    }

    val hash: String by lazy {
        UUID.nameUUIDFromBytes(
            """
              |$id
              |$start
              |$end
              |$title
              |$duration
              |${tags?.size ?: 0}
            """.trimMargin().trimIndent().encodeToByteArray()
        ).toString()
    }
}

@Serializable
data class MetaDataFormat(
    val duration: Double? = null,
    val tags: Map<String, String>? = null
)

/**
 * Analyzes media files for meta data and duration.
 */

data class Metadata(
    val duration: Duration,
    val author: String?,
    val album: String?,
    val albumAuthor: String?,
    val title: String?,
    val chapters: List<MetaDataChapter>,
    val result: MetaDataScanResult,
) {

    val hash: String by lazy {
        UUID.nameUUIDFromBytes(
            """
              |$author
              |$album
              |$albumAuthor
              |$title
              |$duration
              |${chapters.fold(0L.seconds) { acc, c -> acc + c.duration }}
              |${chapters.size}
            """.trimMargin().trimIndent().encodeToByteArray()
        ).toString()
    }
}
