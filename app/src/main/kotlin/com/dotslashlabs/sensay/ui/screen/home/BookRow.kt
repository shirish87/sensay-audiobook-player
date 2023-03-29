package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotslashlabs.sensay.ui.screen.common.BookChaptersDurationInfoRow
import com.dotslashlabs.sensay.ui.screen.common.CoverImage
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookAuthorAndSeries
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookChapter
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.BookTitle
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
fun BookRow(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    config: BookContextMenuConfig,
    onNavToBook: OnNavToBook,
    modifier: Modifier = Modifier,
    onPlay: OnPlay? = null,
    onBookLookup: OnBookLookup? = null,
) {

    var isMenuExpanded by remember { mutableStateOf(false) }
    val setMenuExpanded: (Boolean) -> Unit = { isMenuExpanded = it }

    ElevatedCard(
        modifier = modifier
            .alpha(if (bookProgressWithChapters.book.isActive) 1F else 0.25F)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavToBook(bookProgressWithChapters.book.bookId) },
                onLongClick = { setMenuExpanded(!isMenuExpanded) },
                onDoubleClick = { onBookLookup?.invoke(bookProgressWithChapters.book) },
            ),
    ) {
        Box {
            ListBookView(bookProgressWithChapters)
            BookContextMenu(
                bookProgressWithChapters,
                config,
                isMenuExpanded,
                setMenuExpanded,
                onPlay = onPlay,
                onBookLookup = onBookLookup,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListBookView(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    height: Dp = 140.dp,
    useCoverImage: Boolean = true,
) {

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(maxHeight = height)
    ) {
        if (useCoverImage) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35F),
            ) {
                CoverImage(
                    coverUri = bookProgressWithChapters.book.coverUri,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val book = bookProgressWithChapters.book

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .weight(if (useCoverImage) 0.65F else 1F)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {

            BookAuthorAndSeries(
                book.author,
                book.series,
                book.title,
                maxLengthEach = 25,
                style = MaterialTheme.typography.labelSmall,
            )

            BookTitle(
                bookTitle = book.title,
                bookTitleMaxLines = 3,
            )

            BookChapter(
                chapterTitle = if (bookProgressWithChapters.bookProgress.bookCategory != NOT_STARTED) {
                    bookProgressWithChapters.chapter.title
                } else null,
            )

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
                title = List(1024) { "Book Title" }.joinToString(" "),
                author = List(1024) { "Author" }.joinToString(" "),
                series = List(1024) { "Series" }.joinToString(" "),
                duration = ContentDuration(120.hours + 55.minutes),
            ),
            chapter = Chapter.empty(bookId = 0).copy(
                title = "Chapter 1",
                duration = ContentDuration(1.hours),
            ),
            chapters = listOf(
                Chapter.empty(bookId = 0).copy(
                    title = "Chapter 1",
                    duration = ContentDuration(1.hours),
                ),
                Chapter.empty(bookId = 0).copy(
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
