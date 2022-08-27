package com.dotslashlabs.sensay.ui.screen.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import data.BookCategory
import data.entity.Book
import data.entity.BookProgress

@Composable
fun BookProgressIndicator(
    book: Book,
    bookProgress: BookProgress,
    modifier: Modifier = Modifier,
) {

    if (bookProgress.bookCategory != BookCategory.CURRENT) return

    val bookDurationMs = book.duration.ms
    if (bookDurationMs <= 0) return

    val bookProgressMs = bookProgress.bookProgress.ms
    if (bookProgressMs < 0) return

    val bookProgressFraction = bookProgressMs.toFloat().div(maxOf(1, bookDurationMs))
    if (bookProgressFraction < 0) return

    LinearProgressIndicator(
        progress = bookProgressFraction,
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
    )
}
