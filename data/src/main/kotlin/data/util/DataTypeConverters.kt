package data.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Room [TypeConverter] functions for various classes.
 */
object DataTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromContentDuration(contentDuration: ContentDuration?): Long? {
        return contentDuration?.value?.inWholeMilliseconds
    }

    @TypeConverter
    @JvmStatic
    fun toContentDuration(value: Long?): ContentDuration? {
        return value?.let { ContentDuration(value = it.milliseconds) }
    }

    @TypeConverter
    @JvmStatic
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    @JvmStatic
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(value) }
    }

    @TypeConverter
    @JvmStatic
    fun fromUri(value: Uri?): String? {
        return value?.toString()
    }

    @TypeConverter
    @JvmStatic
    fun toUri(value: String?): Uri? {
        return value?.toUri()
    }
}
