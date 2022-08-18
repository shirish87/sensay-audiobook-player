package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
fun BookCell(
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
        modifier = modifier.fillMaxWidth(),
    ) {
        GridBookView(bookProgressWithChapters)
    }
}

@Composable
private fun GridBookView(
    @Suppress("UNUSED_PARAMETER") bookProgressWithChapters: BookProgressWithBookAndChapters,
    @Suppress("UNUSED_PARAMETER") height: Dp = 270.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .height(height),
    ) {

        CoverImage(
            coverUri = bookProgressWithChapters.book.coverUri,
            modifier = Modifier
                .weight(0.45F),
        )

        val book = bookProgressWithChapters.book

        Column(
            modifier = Modifier
                .weight(0.35F)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        ) {
            book.author?.let { author ->
                Text(
                    text = author.uppercase(),
                    modifier = Modifier.padding(top = 6.dp),
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
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(0.2F)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
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
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
