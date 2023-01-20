package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import data.entity.BookProgressWithBookAndChapters
import kotlin.math.roundToInt

private fun resolveAsyncState(
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
    bottomPadding: Dp = 140.dp,
    onNavToBook: OnNavToBook,
    onPlay: OnPlay? = null,
    onBookLookup: OnBookLookup? = null,
) {

    val state: LazyListState = rememberLazyListState()
    val (isLoading, results) = resolveAsyncState(items)

    val isFilterEnabled = filterMenuOptions.isFilterEnabled || filterListOptions.isFilterEnabled

    var expandedHiddenBooks by remember { mutableStateOf(false) }
    val (allBooks, hiddenBooks) = results.partition { it.bookProgress.isVisible }
    val books = allBooks.filter { !it.isEmpty }

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

            items(books, key = { it.bookProgress.bookProgressId }) { book ->
                BookRow(
                    book,
                    config,
                    onNavToBook = if (book.isEmpty) ({}) else onNavToBook,
                    modifier = Modifier,
                    onPlay = onPlay,
                    onBookLookup = onBookLookup,
                )
            }

            if (hiddenBooks.isNotEmpty()) {
                item {
                    hiddenBooksSummaryView(hiddenBooks, expandedHiddenBooks) {
                        expandedHiddenBooks = it
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        if (expandedHiddenBooks) {
            hiddenBooksListView(
                books = hiddenBooks,
                config = config,
                bottomPadding = bottomPadding,
            ) {
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
    bottomPadding: Dp = 140.dp,
    onNavToBook: OnNavToBook,
    onPlay: OnPlay? = null,
    onBookLookup: OnBookLookup? = null,
) {
    val state: LazyGridState = rememberLazyGridState()
    val cellCount = gridColumnCount()

    val (isLoading, results) = resolveAsyncState(items)

    val lastRowStartCell =
        results.size - (if (results.size % cellCount == 0) cellCount else results.size % cellCount)

    val isFilterEnabled = filterMenuOptions.isFilterEnabled || filterListOptions.isFilterEnabled

    var expandedHiddenBooks by remember { mutableStateOf(false) }
    val (allBooks, hiddenBooks) = results.partition { it.bookProgress.isVisible }
    val books = allBooks.filter { !it.isEmpty }

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

            itemsIndexed(books, key = { _, o -> o.bookProgress.bookProgressId }) { index, book ->
                BookCell(
                    book,
                    config,
                    onNavToBook = if (book.isEmpty) ({}) else onNavToBook,
                    modifier = Modifier.padding(
                        paddingValuesForCell(index + 1, cellCount, lastRowStartCell),
                    ),
                    onPlay = onPlay,
                    onBookLookup = onBookLookup,
                )
            }

            if (hiddenBooks.isNotEmpty()) {
                item(span = { GridItemSpan(cellCount) }) {
                    hiddenBooksSummaryView(hiddenBooks, expandedHiddenBooks) {
                        expandedHiddenBooks = it
                    }
                }
            }

            item(span = { GridItemSpan(cellCount) }) {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        if (expandedHiddenBooks) {
            hiddenBooksGridView(
                books = hiddenBooks,
                config = config,
                cellCount = cellCount,
                bottomPadding = bottomPadding,
            ) {
                expandedHiddenBooks = it
            }
        }
    }
}

private fun paddingValuesForCell(cell: Int, cellCount: Int, lastRowStartCell: Int): PaddingValues {
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
    bottomPadding: Dp = 100.dp,
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

        items(books, key = { it.bookProgress.bookProgressId }) { books ->
            BookRow(
                books,
                config,
                onNavToBook = {},
            )
        }

        item {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun hiddenBooksGridView(
    books: List<BookProgressWithBookAndChapters>,
    cellCount: Int,
    config: BookContextMenuConfig,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 100.dp,
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

        itemsIndexed(books, key = { _, o -> o.bookProgress.bookProgressId }) { index, book ->
            books[index].run {
                BookCell(
                    book,
                    config,
                    onNavToBook = {},
                    modifier = Modifier.padding(
                        paddingValuesForCell(index + 1, cellCount, lastRowStartCell),
                    ),
                )
            }
        }

        item(span = { GridItemSpan(cellCount) }) {
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}
