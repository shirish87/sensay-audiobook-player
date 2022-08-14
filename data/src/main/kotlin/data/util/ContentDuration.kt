package data.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ContentDuration(val value: Duration) {
    companion object {
        val ZERO = ContentDuration(value = 0.milliseconds)
    }

    val ms: Long
        get() = value.inWholeMilliseconds

    fun format() = if (value > ZERO.value) {
        value.toComponents { hh, mm, ss, _ ->
            listOf(hh, mm, ss).joinToString(":") { part ->
                "$part".padStart(2, '0')
            }
        }
    } else {
        ""
    }
}

object ContentDurationParceler : Parceler<ContentDuration> {
    override fun create(parcel: Parcel) = ContentDuration(parcel.readLong().milliseconds)

    override fun ContentDuration.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(ms)
    }
}

object ContentDurationOptParceler : Parceler<ContentDuration?> {
    override fun create(parcel: Parcel) = ContentDuration(parcel.readLong().milliseconds)

    override fun ContentDuration?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeLong(0L)
        } else {
            parcel.writeLong(ms)
        }
    }
}
