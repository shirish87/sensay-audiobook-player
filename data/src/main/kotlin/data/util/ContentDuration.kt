package data.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class ContentDuration(val value: Duration) {
    companion object {
        val ZERO = ContentDuration(value = 0.milliseconds)

        fun parse(contentDurationStr: String): ContentDuration {
            val parts = contentDurationStr.split(":")

            if (parts.size == 3) {
                return ContentDuration(
                    ((parts[0].removePrefix("0").toInt() * 60 * 60) +
                            (parts[1].removePrefix("0").toInt() * 60) +
                            (parts[0].removePrefix("0").toInt())).seconds
                )
            }

            return ZERO
        }
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
}
