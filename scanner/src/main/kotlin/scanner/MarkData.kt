package scanner

import kotlinx.serialization.Serializable

@Serializable
data class MarkData(
    val startMs: Long,
    val endMs: Long,
    val name: String
) : Comparable<MarkData> {
    override fun compareTo(other: MarkData): Int {
        return startMs.compareTo(other.startMs)
    }
}
