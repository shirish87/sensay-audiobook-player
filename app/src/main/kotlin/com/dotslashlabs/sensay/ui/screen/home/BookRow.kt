package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.home.library.OnNavToBook
import data.entity.BookProgressWithBookAndChapters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookRow(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    onNavToBook: OnNavToBook,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = {
            onNavToBook(
                bookProgressWithChapters.book.bookId,
                bookProgressWithChapters.chapter.chapterId
            )
        },
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        ListBookView(bookProgressWithChapters)
    }
}

@Composable
private fun ListBookView(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    height: Dp = 120.dp,
) {

    Row(modifier = Modifier
        .fillMaxSize()
        .height(height)) {
        CoverImage(
            coverUri = bookProgressWithChapters.book.coverUri,
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.3F)
                .sizeIn(height),
        )

        val book = bookProgressWithChapters.book

        Column(
            modifier = Modifier
                .weight(0.7F)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.8F),
            ) {
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
                    lineHeight = 1.2.em,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .weight(0.2F)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = book.duration.format() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (bookProgressWithChapters.bookProgress.totalChapters > 0) {
                    Text(
                        text =  bookProgressWithChapters.bookProgress.chapterProgressDisplayFormat(),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
