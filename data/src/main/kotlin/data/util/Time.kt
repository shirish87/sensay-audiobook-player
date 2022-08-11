package data.util

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class Time(val value: Duration) {
    companion object {
        fun now() = Time(value = Instant.now().toEpochMilli().milliseconds)
        fun zero() = Time(value = 0.milliseconds)
    }

    fun format() = value.toComponents { hh, mm, ss, _ ->
        listOf(hh, mm, ss).joinToString(":") { part ->
            "$part".padStart(2, '0')
        }
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

        other as Time
        if (value != other.value) return false

        return true
    }
}
