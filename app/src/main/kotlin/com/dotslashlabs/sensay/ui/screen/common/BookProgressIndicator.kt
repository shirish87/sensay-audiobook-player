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

    if (bookProgress.bookCategory == BookCategory.NOT_STARTED) return

    val bookDurationMs = book.duration.ms
    val bookProgressMs = bookProgress.bookProgress.ms

    BookProgressIndicator(
        bookProgressMs,
        bookDurationMs,
        modifier,
    )
}

@Composable
fun BookProgressIndicator(
    bookProgressMs: Long,
    bookDurationMs: Long,
    modifier: Modifier = Modifier,
) {

    if (bookDurationMs <= 0) return
    if (bookProgressMs < 0) return

    val bookProgressFraction = bookProgressMs.toFloat().div(maxOf(1, bookDurationMs))
    if (bookProgressFraction < 0) return

    LinearProgressIndicator(
        progress = bookProgressFraction,
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
    )
}
