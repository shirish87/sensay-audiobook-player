package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    height: Dp = 156.dp,
) {

    Row(
        modifier = Modifier.fillMaxWidth()
            .height(height)
    ) {
        Column(
            modifier = Modifier
                .weight(0.3F),
        ) {
            CoverImage(
                coverUri = bookProgressWithChapters.book.coverUri,
            )
        }

        val book = bookProgressWithChapters.book

        Column(
            modifier = Modifier
                .weight(0.7F)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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

            BookChaptersDurationInfoRow(
                book,
                bookProgressWithChapters.bookProgress,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ListBookViewPreview() {
    ListBookView(
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
