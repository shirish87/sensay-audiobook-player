package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotslashlabs.sensay.ui.screen.common.BookChaptersDurationInfoRow
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookAuthorAndSeries
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookChapter
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookTitle
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import data.BookCategory
import data.BookCategory.NOT_STARTED
import data.entity.Book
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import data.entity.Chapter
import data.util.ContentDuration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCell(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    config: BookContextMenuConfig,
    onNavToBook: OnNavToBook,
    modifier: Modifier = Modifier,
) {

    var isMenuExpanded by remember { mutableStateOf(false) }
    val setMenuExpanded: (Boolean) -> Unit = { isMenuExpanded = it }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavToBook(bookProgressWithChapters.book.bookId) },
                onLongClick = { setMenuExpanded(!isMenuExpanded) },
            ),
    ) {
        Box {
            GridBookView(bookProgressWithChapters)
            BookContextMenu(bookProgressWithChapters, config, isMenuExpanded, setMenuExpanded)
        }
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
            .sizeIn(maxHeight = height),
    ) {
        val book = bookProgressWithChapters.book

        Box(
            modifier = Modifier.weight(0.5F),
        ) {
            CoverImage(
                coverUri = bookProgressWithChapters.book.coverUri,
                modifier = Modifier.fillMaxSize(),
            )

            BookAuthorAndSeries(
                book.author,
                book.series,
                book.title,
                maxLengthEach = 25,
                style = MaterialTheme.typography.labelSmall,
                mainAxisAlignment = FlowMainAxisAlignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(authorBackgroundColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .weight(0.5F)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        ) {

            BookTitle(
                bookTitle = book.title,
                bookTitleMaxLines = 3,
            )

            BookChapter(
                chapterTitle = if (bookProgressWithChapters.bookProgress.bookCategory != NOT_STARTED) {
                    bookProgressWithChapters.chapter.title
                } else null,
                chapterTitleMaxLines = 1,
            )

            BookChaptersDurationInfoRow(
                book,
                bookProgressWithChapters.bookProgress,
                useShortDurationFormat = true,
                modifier = Modifier.padding(top = 6.dp),
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
                title = List(1024) { "Book Title" }.joinToString(" "),
                author = List(1024) { "Author" }.joinToString(" "),
                series = List(1024) { "Series" }.joinToString(" "),
                duration = ContentDuration(120.hours + 55.minutes),
            ),
            chapter = Chapter.empty().copy(
                title = List(1024) { "Chapter 1" }.joinToString(" "),
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
