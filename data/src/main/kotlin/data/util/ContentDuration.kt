package data.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlin.time.Duration


val Duration.ms: Long
    get() = this.inWholeMilliseconds

val Duration?.ms: Long?
    get() = this?.inWholeMilliseconds

fun Duration.Companion.ms(valueInMillis: Long) = valueInMillis.milliseconds

fun Duration.Companion.isEmpty(value: Duration?) = (value == null || value == ZERO)

fun Duration.Companion.format(value: Duration?) = if (value != null && value.ms > 0) {
    value.toComponents { hh, mm, ss, _ ->
        listOf(hh, mm, ss).joinToString(":") { part ->
            "$part".padStart(2, '0')
        }
    }
} else {
    null
}

fun Duration.Companion.formatFull(value: Duration?) = if (value != null && value.ms > 0) {
    value.toComponents { hh, mm, ss, _ ->
        listOfNotNull(
            when {
                hh == 1L -> "$hh hr"
                hh > 1 -> "$hh hrs"
                else -> null
            },
            when {
                mm == 1 -> "$mm min"
                mm > 1 -> "$mm mins"
                else -> null
            },
            if (hh == 0L && mm == 0 && ss > 0) {
                "$ss secs"
            } else null,
        ).joinToString(" ")
    }
} else {
    null
}

fun Duration.Companion.formatShort(value: Duration?) = if (value != null && value.ms > 0) {
    value.toComponents { hh, mm, ss, _ ->
        listOfNotNull(
            if (hh > 0) "${hh}h" else null,
            if (mm > 0) "${mm}m" else null,
            if (hh == 0L && mm == 0 && ss > 0) "${ss}s" else null,
        ).joinToString(" ")
    }
} else {
    null
}

fun Duration?.isEmpty() = Duration.isEmpty(this)
fun Duration?.format() = Duration.format(this)
fun Duration?.formatFull() = Duration.formatFull(this)
fun Duration?.formatShort() = Duration.formatShort(this)

data class ContentDuration(val value: Duration) : Comparable<ContentDuration> {
    companion object {
        val ZERO = ContentDuration(Duration.ZERO)

        fun ms(valueInMillis: Long) = ContentDuration(Duration.ms(valueInMillis))
        fun format(contentDuration: ContentDuration?) = Duration.format(contentDuration?.value)
        fun format(duration: Duration?) = Duration.format(duration)
    }

    val ms = value.ms
    fun format() = value.format()
    fun formatFull() = value.formatFull()
    fun formatShort() = value.formatShort()
    fun isEmpty() = value.isEmpty()

    override fun compareTo(other: ContentDuration): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return value == (other as? ContentDuration?)?.value
    }

    override fun hashCode(): Int = value.hashCode()
}

object ContentDurationParceler : Parceler<ContentDuration> {
    override fun create(parcel: Parcel) = ContentDuration.ms(parcel.readLong())

    override fun ContentDuration.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(ms)
    }
}

object ContentDurationOptParceler : Parceler<ContentDuration?> {
    override fun create(parcel: Parcel) = ContentDuration.ms(parcel.readLong())

    override fun ContentDuration?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeLong(ContentDuration.ZERO.ms)
        } else {
            parcel.writeLong(ms)
        }
    }
}
