package com.dotslashlabs.sensay.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.dotslashlabs.sensay.ui.screen.common.BookProgressIndicator
import data.BookCategory
import data.entity.Book
import data.entity.BookProgress
import data.util.ContentDuration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


@Composable
fun BookChaptersDurationInfoRow(
    book: Book,
    bookProgress: BookProgress,
    useShortDurationFormat: Boolean = false,
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
                },
            )
        }

        with (book.duration) {
            if (useShortDurationFormat)
                formatShort()
            else
                formatFull()
        }?.let { duration ->
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
                },
            )
        }

        BookProgressIndicator(
            book = book,
            bookProgress = bookProgress,
            modifier = Modifier.constrainAs(progress) {
                top.linkTo(text1.bottom, margin = 10.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.fillToConstraints
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun BookChaptersDurationInfoRowPreview() {
    BookChaptersDurationInfoRow(
        book = Book.empty().copy(
            title = "Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title Book Title",
            author = "Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author Author",
            duration = ContentDuration(2.hours + 55.minutes),
        ),
        bookProgress = BookProgress.empty().copy(
            bookCategory = BookCategory.CURRENT,
            currentChapter = 1,
            totalChapters = 2,
            bookProgress = ContentDuration(1.hours + 55.minutes),
        ),
    )
}
