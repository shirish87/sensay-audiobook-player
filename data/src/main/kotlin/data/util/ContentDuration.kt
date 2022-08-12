package data.util

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class ContentDuration(val value: Duration) {
    companion object {
        val ZERO = ContentDuration(value = 0.milliseconds)
    }

    fun format() = if (value > ZERO.value) {
        value.toComponents { hh, mm, ss, _ ->
            listOf(hh, mm, ss).joinToString(":") { part ->
                "$part".padStart(2, '0')
            }
        }
    } else {
        ""
    }

    override fun toString(): String {
        return Instant.ofEpochMilli(value.inWholeMilliseconds).toString()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContentDuration
        if (value != other.value) return false

        return true
    }
}
