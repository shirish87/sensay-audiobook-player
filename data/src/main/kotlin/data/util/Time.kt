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
}
