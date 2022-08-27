package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.dotslashlabs.sensay.ui.screen.BookChaptersDurationInfoRow
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.home.library.OnNavToBook
import data.BookCategory
import data.entity.Book
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.Chapter
import data.util.ContentDuration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    height: Dp = 250.dp,
    authorBackgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85F),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val book = bookProgressWithChapters.book

        Box(
            modifier = Modifier.weight(0.5F),
        ) {
            CoverImage(
                coverUri = bookProgressWithChapters.book.coverUri,
            )

            book.author?.let { author ->
                Text(
                    text = author.uppercase(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(authorBackgroundColor)
                        .padding(horizontal = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(0.5F)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {

            BookChaptersDurationInfoRow(
                book,
                bookProgressWithChapters.bookProgress,
                useShortDurationFormat = true,
                modifier = Modifier.padding(vertical = 6.dp),
            )

            Text(
                text = book.title,
                lineHeight = 1.2.em,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
@Preview(showBackground = true)
private fun GridBookViewPreview() {
    GridBookView(
        bookProgressWithChapters = BookProgressWithBookAndChapters(
            book = Book.empty().copy(
                title = "Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title",
                author = "Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author",
                duration = ContentDuration(120.hours + 55.minutes),
            ),
            chapter = Chapter.empty().copy(
                title = "Chapter 1",
                duration = ContentDuration(1.hours),
            ),
            chapters = listOf(
                Chapter.empty().copy(
                    title = "Chapter 1",
                    duration = ContentDuration(1.hours),
                ),
                Chapter.empty().copy(
                    title = "Chapter 2",
                    duration = ContentDuration(1.hours),
                ),
            ),
            bookProgress = BookProgress.empty().copy(
                bookCategory = BookCategory.CURRENT,
                currentChapter = 1,
                totalChapters = 2,
                bookProgress = ContentDuration(1.hours),
            ),
        ),
    )
}
