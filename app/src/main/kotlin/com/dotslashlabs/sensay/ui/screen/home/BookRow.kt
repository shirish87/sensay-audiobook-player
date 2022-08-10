package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import data.entity.BookProgressWithBookAndChapters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRow(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    onNavToBook: (bookId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = { onNavToBook(bookProgressWithChapters.book.bookId) },
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        BookView(bookProgressWithChapters)
    }
}

@Composable
private fun BookView(bookProgressWithChapters: BookProgressWithBookAndChapters) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        CoverImage(
            coverUri = bookProgressWithChapters.book.coverUri,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Column(
            modifier = Modifier.padding(start = 12.dp)
        ) {
            val book = bookProgressWithChapters.book

            book.author?.let { author ->
                Text(
                    text = author.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = book.title,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Row {
                Text(
                    text = book.duration.format(),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (bookProgressWithChapters.bookProgress.totalChapters > 0) {
                    Text(
                        text = "${
                            listOf(
                                bookProgressWithChapters.bookProgress.currentChapter,
                                bookProgressWithChapters.bookProgress.totalChapters,
                            ).joinToString(separator = " / ")
                        } chapters",
                        modifier = Modifier.padding(top = 6.dp, start = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

