package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import data.entity.BookProgressWithBookAndChapters
import kotlin.math.roundToInt


@Composable
fun BooksList(
    books: List<BookProgressWithBookAndChapters>,
    onNavToBook: (bookId: Long) -> Unit,
) {
    val state: LazyListState = rememberLazyListState()

    LazyColumn(state = state) {
        items(count = books.size) { index ->
            BookRow(
                books[index],
                onNavToBook,
                modifier = Modifier,
            )
        }
    }
}

@Composable
fun BooksGrid(
    books: List<BookProgressWithBookAndChapters>,
    onNavToBook: (bookId: Long) -> Unit,
) {
    val state: LazyGridState = rememberLazyGridState()
    val cellCount = gridColumnCount()

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(cellCount),
    ) {
        if (books.isEmpty()) return@LazyVerticalGrid

        val defaultPadding = 6.dp
        val borderPadding = 16.dp
        val bottomEdgePadding = 16.dp

        items(count = books.size) { index ->
            val cell = index + 1

            val topPadding = if (cell <= cellCount) borderPadding else defaultPadding
            val bottomPadding = if (cell > books.size - (books.size % cellCount))
                bottomEdgePadding
            else defaultPadding

            val paddingValues: PaddingValues = if (cell % cellCount == 1) {
                PaddingValues(start = borderPadding, end = defaultPadding, top = topPadding, bottom = bottomPadding)
            } else if (cell % cellCount == 0) {
                PaddingValues(start = defaultPadding, end = borderPadding, top = topPadding, bottom = bottomPadding)
            } else {
                PaddingValues(start = defaultPadding, end = defaultPadding, top = topPadding, bottom = bottomPadding)
            }

            BookCell(
                books[index],
                onNavToBook,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun gridColumnCount(): Int {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val widthPx = displayMetrics.widthPixels.toFloat()
    val desiredPx = with(LocalDensity.current) {
        180.dp.toPx()
    }
    val columns = (widthPx / desiredPx).roundToInt()
    return columns.coerceAtLeast(2)
}
