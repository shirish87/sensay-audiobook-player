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
import androidx.compose.ui.unit.em
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import data.entity.BookProgressWithBookAndChapters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCell(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    onNavToBook: (bookId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = { onNavToBook(bookProgressWithChapters.book.bookId) },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            CoverImage(
                coverUri = bookProgressWithChapters.book.coverUri,
                modifier = Modifier
                    .sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
                    .clip(RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)),
            )

            Row {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 6.dp, bottom = 12.dp),
                ) {
                    val book = bookProgressWithChapters.book

                    book.author?.let { author ->
                        Text(
                            text = author.uppercase(),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = book.title,
                        lineHeight = 1.2.em,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = book.duration.format(),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
