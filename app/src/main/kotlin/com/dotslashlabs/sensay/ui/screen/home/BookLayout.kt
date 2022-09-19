package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
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
        false to items()
    }
    is Loading -> {
        true to List(emptyListItemsCount) { BookProgressWithBookAndChapters.empty() }
    }
    else -> {
        false to emptyList()
    }
}

@Composable
fun <SortMenuType> BooksList(
    items: Async<List<BookProgressWithBookAndChapters>>,
    config: BookContextMenuConfig,
    sortMenuOptions: SortMenuOptions<SortMenuType>,
    filterMenuOptions: FilterMenuOptions,
    filterListOptions: FilterListOptions<String>,
    onNavToBook: OnNavToBook,
) {

    val state: LazyListState = rememberLazyListState()
    val (isLoading, results) = resolveAsyncState(items)

    val isFilterEnabled = filterMenuOptions.isFilterEnabled || filterListOptions.isFilterEnabled

    Column(Modifier.fillMaxSize()) {
        if (isFilterEnabled || (!isLoading && results.isNotEmpty())) {
            FilterBar(
                sortMenuOptions = sortMenuOptions,
                filterMenuOptions = filterMenuOptions,
                filterListOptions = filterListOptions,
            )
        }

        LazyColumn(state = state, modifier = Modifier.weight(1F)) {
            if (results.isEmpty() && isFilterEnabled) {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "Nothing found.",
                    )
                }
            }

            val (books, hiddenBooks) = results.partition { it.bookProgress.isVisible }

            items(count = books.size) { index ->
                books[index].run {
                    BookRow(
                        this,
                        config,
                        onNavToBook = if (isEmpty) ({}) else onNavToBook,
                        modifier = Modifier,
                    )
                }
            }

            if (hiddenBooks.isNotEmpty()) {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "${hiddenBooks.size} books are hidden.",
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
fun <SortMenuType> BooksGrid(
    items: Async<List<BookProgressWithBookAndChapters>>,
    config: BookContextMenuConfig,
    sortMenuOptions: SortMenuOptions<SortMenuType>,
    filterMenuOptions: FilterMenuOptions,
    filterListOptions: FilterListOptions<String>,
    onNavToBook: OnNavToBook,
) {
    val state: LazyGridState = rememberLazyGridState()
    val cellCount = gridColumnCount()

    val (isLoading, results) = resolveAsyncState(items)

    val lastRowStartCell =
        results.size - (if (results.size % cellCount == 0) cellCount else results.size % cellCount)

    val isFilterEnabled = filterMenuOptions.isFilterEnabled || filterListOptions.isFilterEnabled

    Column(Modifier.fillMaxSize()) {
        if (isFilterEnabled || (!isLoading && results.isNotEmpty())) {
            FilterBar(
                sortMenuOptions = sortMenuOptions,
                filterMenuOptions = filterMenuOptions,
                filterListOptions = filterListOptions,
            )
        }

        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(cellCount),
            modifier = Modifier
                .fillMaxSize()
                .weight(1F),
        ) {
            if (results.isEmpty() && isFilterEnabled) {
                item(span = { GridItemSpan(cellCount) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "Nothing found.",
                    )
                }
            }

            val (books, hiddenBooks) = results.partition { it.bookProgress.isVisible }

            items(count = books.size) { index ->
                books[index].run {
                    BookCell(
                        this,
                        config,
                        onNavToBook = if (isEmpty) ({}) else onNavToBook,
                        modifier = Modifier.padding(
                            paddingValuesForCell(index + 1, cellCount, lastRowStartCell),
                        ),
                    )
                }
            }

            if (hiddenBooks.isNotEmpty()) {
                item(span = { GridItemSpan(cellCount) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "${hiddenBooks.size} books are hidden.",
                    )
                }
            }
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
