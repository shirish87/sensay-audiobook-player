package scanner

import android.content.Context
import android.net.Uri
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.asLog
import logcat.logcat
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class MediaAnalyzer @Inject constructor() {

  private val json = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
  }

  suspend fun analyze(context: Context, uri: Uri): Metadata? {
    logcat { "analyze $uri" }

    val result = ffprobe(
      input = uri,
      context = context,
      command = listOf(
        "-print_format", "json=c=1",
        "-show_chapters",
        "-loglevel", "quiet",
        "-show_entries", "format=duration",
        "-show_entries", "format_tags=artist,album_artist,title,album,genre",
        "-show_entries", "stream_tags=artist,title,album,language",
        "-select_streams", "a" // only select the audio stream
      )
    )
    if (result == null) {
      logcat { "Unable to parse $uri." }
      return null
    }

    val parsed = try {
      json.decodeFromString(MetaDataScanResult.serializer(), result)
    } catch (e: SerializationException) {
      logcat { "Unable to parse $uri: ${e.asLog()}" }
      return null
    }

    val duration = parsed.format?.duration
    return if (duration != null && duration > 0) {
      Metadata(
        result = parsed,
        duration = duration.seconds,
        author = parsed.findTag(TagType.Artist) ?: parsed.findTag(TagType.AlbumArtist),
        album = parsed.findTag(TagType.Album),
        title = parsed.findTag(TagType.Title) ?: "",
        chapters = parsed.chapters,
      )
    } else {
      logcat { "Unable to parse $uri" }
      null
    }
  }
}

private fun nameFromFileUri(file: Uri): String {
  val name = file.lastPathSegment ?: "Book"
  return name.substringBeforeLast(".")
    .trim()
    .takeUnless { it.isEmpty() }
    ?: name
}
