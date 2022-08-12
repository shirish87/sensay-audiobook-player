package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
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
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import data.entity.BookProgressWithBookAndChapters
import kotlin.math.roundToInt


fun resolveAsyncState(
    items: Async<List<BookProgressWithBookAndChapters>>,
    emptyListItemsCount: Int = 12,
) = when (items) {
    is Success -> {
        items()
    }
    is Loading -> {
        List(emptyListItemsCount) { BookProgressWithBookAndChapters.empty() }
    }
    else -> {
        emptyList()
    }
}

@Composable
fun BooksList(
    items: Async<List<BookProgressWithBookAndChapters>>,
    onNavToBook: (bookId: Long) -> Unit,
) {
    val state: LazyListState = rememberLazyListState()
    val books = resolveAsyncState(items)

    LazyColumn(state = state) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(count = books.size) { index ->
            BookRow(
                books[index],
                onNavToBook,
                modifier = Modifier,
            )
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun BooksGrid(
    items: Async<List<BookProgressWithBookAndChapters>>,
    onNavToBook: (bookId: Long) -> Unit,
) {
    val state: LazyGridState = rememberLazyGridState()
    val cellCount = gridColumnCount()

    val books = resolveAsyncState(items)

    val lastRowStartCell =
        books.size - (if (books.size % cellCount == 0) cellCount else books.size % cellCount)

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(cellCount),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (books.isEmpty()) return@LazyVerticalGrid

        items(count = books.size) { index ->
            BookCell(
                books[index],
                onNavToBook,
                modifier = Modifier.padding(
                    paddingValuesForCell(index + 1, cellCount, lastRowStartCell),
                ),
            )
        }
    }
}

fun paddingValuesForCell(cell: Int, cellCount: Int, lastRowStartCell: Int): PaddingValues {
    val startPadding = if (cell % cellCount == 1) 16.dp else 0.dp

    return if (cell <= cellCount) {
        // first row
        PaddingValues(top = 16.dp, start = startPadding, bottom = 16.dp, end = 16.dp)
    } else if (cell > lastRowStartCell) {
        // last row
        PaddingValues(start = startPadding, bottom = 36.dp, end = 16.dp)
    } else {
        PaddingValues(start = startPadding, bottom = 16.dp, end = 16.dp)
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
