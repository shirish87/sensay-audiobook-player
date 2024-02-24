package media

import android.net.Uri
import com.dotslashlabs.media3.extractor.m4b.metadata.ChapterMetadata
import com.dotslashlabs.media3.extractor.m4b.metadata.M4bMetadata
import data.SourceBookWithChaptersAndTags
import data.entity.Book
import data.entity.BookId
import data.entity.BookWithChapters
import data.entity.Chapter
import data.entity.SourceId
import data.util.ContentDuration
import logcat.logcat
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.microseconds

data class SourceFile(
    val sourceId: SourceId,
    val metadata: M4bMetadata,
    val chapters: List<Triple<Uri, ChapterMetadata, M4bMetadata?>>,
    val scanInstant: Instant = Instant.now(),
) {

    // if the same file exists with a different file name, we need to de-dup it
    val hash: String by lazy {
        UUID.nameUUIDFromBytes(
            """
              |${metadata.durationUs}
              |${chapters.joinToString(",") { it.second.startTime.toString() }}
              |${chapters.size}
              |${metadata.artist}
              |${metadata.title}
              |${metadata.albumArtist}
              |${metadata.albumTitle}
              |${metadata.artworkDataType ?: 0}
              |${metadata.artworkData?.size ?: 0}
              |${metadata.artworkUri?.toString()}
            """.trimMargin().trimIndent().encodeToByteArray()
        ).toString()
    }

    fun toBookWithChaptersAndTags(
        bookId: BookId = 0,
        coverMap: Map<String, Pair<Uri, Int>> = emptyMap(),
    ): SourceBookWithChaptersAndTags {

        val title = "${metadata.albumTitle ?: metadata.title}"
        val author = "${metadata.albumArtist ?: metadata.artist}"
        val duration = metadata.durationUs?.microseconds?.inWholeMilliseconds ?: 0
        var trackId = 0

        val chapterList = buildChapters(
            duration, chapters
        ) { idx, uri, chapter, startTime, endTime, chapterDuration ->
            val cover = if (coverMap.isEmpty()) null
            else coverMap.getOrElse("${hash}-${idx}") {
                logcat { "Cover not found for ${hash}-${idx}" }
                null
            }

            Chapter(
                bookId = bookId,
                uri = uri,
                trackId = trackId,
                title = chapter.chapterTitle ?: "${trackId + 1}",
                duration = ContentDuration.ms(chapterDuration),
                start = ContentDuration.ms(startTime),
                end = ContentDuration.ms(endTime),
                coverUri = cover?.first,
                srcCoverByteSize = cover?.second,
                scanInstant = scanInstant,
            ).also {
                trackId++
            }
        }

        return SourceBookWithChaptersAndTags(
            sourceId = sourceId,
            bookWithChapters = BookWithChapters(
                book = Book(
                    bookId = bookId,
                    hash = hash,
                    title = title,
                    author = author,
                    duration = ContentDuration.ms(duration),
                    series = (metadata.compilation?.toString() ?: metadata.albumTitle)?.takeIf {
                        it.trim().isNotEmpty() && it != title
                    }.let { "${metadata.albumTitle}" },
                    narrator = metadata.composer?.toString(),
                    description = metadata.description?.toString(),
                    isActive = true,
                    inactiveReason = null,
                    scanInstant = scanInstant,
                ),
                chapters = chapterList,
            ),
            tags = emptySet(),
        )
    }
}
