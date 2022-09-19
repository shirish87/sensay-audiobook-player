package data

import data.entity.BookProgressId
import java.time.Instant

data class BookProgressVisibility(
    val bookProgressId: BookProgressId,
    val isVisible: Boolean,
    val lastUpdatedAt: Instant = Instant.now(),
)
