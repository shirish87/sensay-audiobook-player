package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

    var expandedHiddenBooks by remember { mutableStateOf(false) }
    val (books, hiddenBooks) = results.partition { it.bookProgress.isVisible }

    Column(Modifier.fillMaxSize()) {
        if (isFilterEnabled || (!isLoading && results.isNotEmpty())) {
            FilterBar(
                sortMenuOptions = sortMenuOptions,
                filterMenuOptions = filterMenuOptions,
                filterListOptions = filterListOptions,
            )
        }

        LazyColumn(state = state, modifier = Modifier.weight(1F)) {
            if (books.isEmpty() && isFilterEnabled) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "Nothing found.",
                    )
                }
            }

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
                    hiddenBooksSummaryView(hiddenBooks, expandedHiddenBooks) {
                        expandedHiddenBooks = it
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        if (expandedHiddenBooks) {
            hiddenBooksListView(books = hiddenBooks, config = config) {
                expandedHiddenBooks = it
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

    var expandedHiddenBooks by remember { mutableStateOf(false) }
    val (books, hiddenBooks) = results.partition { it.bookProgress.isVisible }

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
            if (books.isEmpty() && isFilterEnabled) {
                item(span = { GridItemSpan(cellCount) }) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = TextAlign.Center,
                        text = "Nothing found.",
                    )
                }
            }

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
                    hiddenBooksSummaryView(hiddenBooks, expandedHiddenBooks) {
                        expandedHiddenBooks = it
                    }
                }
            }
        }

        if (expandedHiddenBooks) {
            hiddenBooksGridView(books = hiddenBooks, config = config, cellCount = cellCount) {
                expandedHiddenBooks = it
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun hiddenBooksSummaryView(
    hiddenBooks: List<BookProgressWithBookAndChapters>,
    expanded: Boolean,
    setExpanded: (Boolean) -> Unit,
) {

    OutlinedCard(
        onClick = { setExpanded(!expanded) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${hiddenBooks.size} ${
                if (hiddenBooks.size == 1)
                    "book is"
                else "books are"
                } hidden.",
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (expanded)
                    Icons.Outlined.ExpandLess
                else
                    Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun hiddenBooksListView(
    books: List<BookProgressWithBookAndChapters>,
    config: BookContextMenuConfig,
    modifier: Modifier = Modifier,
    setExpanded: (Boolean) -> Unit,
) {

    val state: LazyListState = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
    ) {

        item {
            hiddenBooksSummaryView(books, true, setExpanded)
        }

        items(count = books.size) { index ->
            books[index].run {
                BookRow(
                    this,
                    config,
                    onNavToBook = {},
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun hiddenBooksGridView(
    books: List<BookProgressWithBookAndChapters>,
    cellCount: Int,
    config: BookContextMenuConfig,
    modifier: Modifier = Modifier,
    setExpanded: (Boolean) -> Unit,
) {

    val lastRowStartCell =
        books.size - (if (books.size % cellCount == 0) cellCount else books.size % cellCount)

    val state: LazyGridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Fixed(cellCount),
        modifier = modifier.fillMaxSize(),
    ) {

        item(span = { GridItemSpan(cellCount) }) {
            hiddenBooksSummaryView(books, true, setExpanded)
        }

        items(count = books.size) { index ->
            books[index].run {
                BookCell(
                    this,
                    config,
                    onNavToBook = {},
                    modifier = Modifier.padding(
                        paddingValuesForCell(index + 1, cellCount, lastRowStartCell),
                    ),
                )
            }
        }
    }
}
