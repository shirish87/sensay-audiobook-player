package com.dotslashlabs.sensay.ui.screen.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import com.dotslashlabs.sensay.R
import com.dotslashlabs.sensay.common.joinWith
import compose.icons.MaterialIcons
import compose.icons.materialicons.ListAlt
import compose.icons.materialicons.Timer
import config.HomeLayout
import data.BookCategory
import data.entity.Book
import data.entity.BookProgress
import data.entity.BookProgressWithBookAndChapters
import logcat.asLog
import logcat.logcat


@Composable
fun BookCell(
    homeLayout: HomeLayout,
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    config: BookContextMenuConfig,
    isMenuExpanded: Boolean,
    setMenuExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: OnPlay? = null,
    onBookLookup: OnBookLookup? = null,
) {

    ElevatedCard(modifier = modifier) {
        Box {
            when (homeLayout) {
                HomeLayout.GRID -> GridBookView(bookProgressWithChapters)
                HomeLayout.LIST -> ListBookView(bookProgressWithChapters)
            }

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
                coverUri = bookProgressWithChapters.chapter.coverUri,
                modifier = Modifier.fillMaxSize(),
            )

            BookAuthorAndSeries(
                book.author,
                book.series,
                book.title,
                maxLengthEach = 25,
                style = MaterialTheme.typography.labelSmall,
                horizontalArrangement = Arrangement.Center,
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
                bookTitleMaxLines = 4,
            )

            BookChapter(
                chapterTitle = if (bookProgressWithChapters.bookProgress.bookCategory != BookCategory.NOT_STARTED) {
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
            .sizeIn(maxHeight = height),
    ) {
        if (useCoverImage) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35F),
            ) {
                CoverImage(
                    coverUri = bookProgressWithChapters.chapter.coverUri,
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
                chapterTitle = if (bookProgressWithChapters.bookProgress.bookCategory != BookCategory.NOT_STARTED) {
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
fun BookChaptersDurationInfoRow(
    book: Book,
    bookProgress: BookProgress,
    modifier: Modifier = Modifier,
    useShortDurationFormat: Boolean = false,
) {

    ConstraintLayout(
        modifier = modifier.fillMaxWidth(),
    ) {
        val (icon1, text1, icon2, text2, progress) = createRefs()

        if (bookProgress.totalChapters > 0) {
            Icon(
                MaterialIcons.ListAlt,
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

        with(book.duration) {
            if (useShortDurationFormat)
                formatShort()
            else
                formatFull()
        }?.let { duration ->
            Icon(
                MaterialIcons.Timer,
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
                bottom.linkTo(text1.top, margin = 10.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.fillToConstraints
            },
        )
    }
}

@Composable
fun BookProgressIndicator(
    book: Book,
    bookProgress: BookProgress,
    modifier: Modifier = Modifier,
) {

    if (bookProgress.bookCategory == BookCategory.NOT_STARTED) return

    val bookDurationMs = book.duration.ms
    val bookProgressMs = bookProgress.bookProgress.ms

    BookProgressIndicator(
        bookProgressMs,
        bookDurationMs,
        modifier,
    )
}

@Composable
fun BookProgressIndicator(
    bookProgressMs: Long,
    bookDurationMs: Long,
    modifier: Modifier = Modifier,
) {

    if (bookDurationMs <= 0) return
    if (bookProgressMs < 0) return

    val bookProgressFraction = bookProgressMs.toFloat().div(maxOf(1, bookDurationMs))
    if (bookProgressFraction < 0) return

    LinearProgressIndicator(
        progress = { bookProgressFraction },
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
    )
}


@Composable
fun CoverImage(
    coverUri: Uri?,
    modifier: Modifier = Modifier,
    drawableResId: Int = R.drawable.empty,
) {
    AsyncImage(
        model = coverUri ?: drawableResId,
        placeholder = painterResource(drawableResId),
        fallback = painterResource(drawableResId),
        onError = { error ->
            logcat("AsyncImage") {
                "Error loading cover image $coverUri: ${
                    error.result.throwable.asLog()
                }"
            }
        },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
@ExperimentalLayoutApi
fun BookAuthorAndSeries(
    author: String?,
    series: String?,
    bookTitle: String,
    modifier: Modifier = Modifier,
    maxLengthEach: Int = 40,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
) {

    if (author == null && series == null) return

    FlowRow(
        modifier = modifier.fillMaxWidth(0.95F),
        horizontalArrangement = horizontalArrangement,
    ) {
        listOfNotNull(
            author,
            if (series == bookTitle)
                null
            else series,
        )
            .joinWith(" ${Typography.bullet} ")
            .forEach { text ->
                Text(
                    text = text.run {
                        if (length > maxLengthEach)
                            "${substring(0, maxLengthEach)}${Typography.ellipsis}"
                        else this
                    },
                    style = style,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

//@Composable
//fun BookTitleAndChapterFlow(
//    nowPlayingBook: BookProgressWithDuration,
//    modifier: Modifier = Modifier,
//    sourceStyle: TextStyle = MaterialTheme.typography.labelSmall,
//    mainStyle: TextStyle = MaterialTheme.typography.titleSmall,
//) {
//
//    Column(modifier = modifier.fillMaxSize()) {
//        Text(
//            text = listOfNotNull(
//                nowPlayingBook.author,
//                nowPlayingBook.series,
//            ).joinToString(" ${Typography.bullet} ") { it.trim() },
//            style = sourceStyle,
//            maxLines = 2,
//            overflow = TextOverflow.Ellipsis,
//        )
//
//        Text(
//            modifier = Modifier.padding(top = 4.dp),
//            text = listOfNotNull(
//                nowPlayingBook.chapterTitle,
//                if (nowPlayingBook.bookTitle != nowPlayingBook.series)
//                    nowPlayingBook.bookTitle
//                else null,
//            ).joinToString(" ${Typography.bullet} ") { it.trim() },
//            style = mainStyle,
//            maxLines = 2,
//            overflow = TextOverflow.Ellipsis,
//        )
//    }
//}
//
//@Composable
//fun BookTitleAndChapter(
//    bookTitle: String,
//    chapterTitle: String?,
//    bookTitleMaxLines: Int = 2,
//    chapterTitleMaxLines: Int = 1,
//    bookTitleStyle: TextStyle = MaterialTheme.typography.titleSmall,
//    chapterTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
//) {
//
//    Text(
//        modifier = Modifier.padding(top = 2.dp),
//        text = bookTitle.trim(),
//        maxLines = bookTitleMaxLines,
//        overflow = TextOverflow.Ellipsis,
//        style = bookTitleStyle,
//    )
//
//    if (chapterTitle != null) {
//        Text(
//            modifier = Modifier.padding(top = 2.dp),
//            text = chapterTitle.trim(),
//            maxLines = chapterTitleMaxLines,
//            overflow = TextOverflow.Ellipsis,
//            style = chapterTitleStyle,
//        )
//    }
//}

@Composable
fun BookChapter(
    chapterTitle: String?,
    modifier: Modifier = Modifier,
    chapterTitleMaxLines: Int = 1,
    chapterTitleStyle: TextStyle = MaterialTheme.typography.labelSmall,
) {

    if (chapterTitle != null) {
        Text(
            modifier = modifier.padding(top = 2.dp),
            text = chapterTitle.trim(),
            maxLines = chapterTitleMaxLines,
            overflow = TextOverflow.Ellipsis,
            style = chapterTitleStyle,
        )
    }
}

@Composable
fun BookTitle(
    bookTitle: String,
    bookTitleMaxLines: Int = 2,
    bookTitleStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {

    Text(
        modifier = Modifier.padding(top = 2.dp),
        text = bookTitle.trim(),
        maxLines = bookTitleMaxLines,
        overflow = TextOverflow.Ellipsis,
        style = bookTitleStyle,
    )
}

//fun resolveContentMaxHeight(
//    bookProgressWithDuration: BookProgressWithDuration,
//    heightLow: Dp = 96.dp,
//    heightHigh: Dp = 110.dp,
//    charThreshold: Int = 80,
//): Dp {
//
//    return bookProgressWithDuration.run {
//        val contentLength = listOfNotNull(
//            author,
//            if (series != bookTitle) series else null,
//            bookTitle,
//            chapterTitle,
//        ).joinToString(" ").length
//
//        if (contentLength > charThreshold)
//            heightHigh
//        else heightLow
//    }
//}
