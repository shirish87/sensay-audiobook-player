package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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
                        .background(authorBackgroundColor),
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
fun BookChaptersDurationInfoRow(
    book: Book,
    bookProgress: BookProgress,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier.fillMaxWidth(),
    ) {
        val (icon1, text1, icon2, text2, progress) = createRefs()

        if (bookProgress.totalChapters > 0) {
            Icon(
                Icons.Outlined.ListAlt,
                modifier = Modifier
                    .alpha(0.65F)
                    .constrainAs(icon1) {
                        top.linkTo(text1.top)
                        bottom.linkTo(text1.bottom)

                        start.linkTo(parent.start)
                        height = Dimension.fillToConstraints
                    },
                contentDescription = null,
            )

            Text(
                textAlign = TextAlign.Start,
                text = bookProgress.chapterProgressDisplayFormat().replace(" chapters", ""),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(text1) {
                    start.linkTo(icon1.end, margin = 4.dp)
                }
            )
        }

        book.duration.formatFull()?.let { duration ->
            Icon(
                Icons.Outlined.Timer,
                modifier = Modifier
                    .alpha(0.65F)
                    .constrainAs(icon2) {
                        top.linkTo(text2.top)
                        bottom.linkTo(text2.bottom)

                        end.linkTo(text2.start, margin = 4.dp)
                        height = Dimension.fillToConstraints
                    },
                contentDescription = null,
            )

            Text(
                textAlign = TextAlign.Start,
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.constrainAs(text2) {
                    end.linkTo(parent.end)
                }
            )
        }

        if (bookProgress.bookCategory == BookCategory.CURRENT) {
            val bookProgressFraction = bookProgress.bookProgress.ms
                .toFloat()
                .div(maxOf(1, book.duration.ms))

            if (bookProgressFraction > 0) {
                LinearProgressIndicator(
                    progress = bookProgressFraction,
                    modifier = Modifier
                        .constrainAs(progress) {
                            top.linkTo(text1.bottom, margin = 10.dp)
                            linkTo(start = parent.start, end = parent.end)
                        }
                )
            }
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
                currentChapter = 1,
                totalChapters = 2,
            ),
        ),
    )
}

@Composable
@Preview(showBackground = true)
private fun BookChaptersDurationInfoRowPreview() {
    BookChaptersDurationInfoRow(
        book = Book.empty().copy(
            title = "Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title",
            author = "Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author",
            duration = ContentDuration(120.hours + 55.minutes),
        ),
        bookProgress = BookProgress.empty().copy(
            currentChapter = 1,
            totalChapters = 2,
        ),
    )
}
