package data.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import kotlin.time.Duration.Companion.milliseconds

/**
 * Room [TypeConverter] functions for various classes.
 */
object DataTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromTime(time: Time?): Long? {
        return time?.value?.inWholeMilliseconds
    }

    @TypeConverter
    @JvmStatic
    fun toTime(value: Long?): Time? {
        return value?.let { Time(value = it.milliseconds) }
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
