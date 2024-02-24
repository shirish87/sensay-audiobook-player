package com.dotslashlabs.sensay.ui.screen.home

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ui.common.LocalWindowSize
import com.dotslashlabs.sensay.ui.common.ScannerViewModel
import com.dotslashlabs.sensay.ui.common.ScannerViewState
import com.dotslashlabs.sensay.ui.common.SensayScaffold
import com.dotslashlabs.sensay.ui.nav.AppScreen
import com.dotslashlabs.sensay.ui.nav.Destination
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingView
import com.dotslashlabs.sensay.ui.nowplaying.NowPlayingViewModel
import com.dotslashlabs.sensay.ui.theme.SpacerSystemBarsBottomPadding
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.launch
import logcat.logcat
import kotlin.math.roundToInt

object HomeScreen : AppScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @UnstableApi
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val scannerViewModel: ScannerViewModel = mavericksActivityViewModel()
        val isScanningFolders by scannerViewModel.collectAsState(ScannerViewState::isScanningFolders)

        val nowPlayingViewModel: NowPlayingViewModel = mavericksActivityViewModel()
        val viewModel: HomeViewModel = mavericksViewModel()
        val viewState by viewModel.collectAsState()
        val context: Context = LocalContext.current

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        BackHandler(viewState.isBackEnabled) {
            viewModel.setLocationToUp()
        }

        SensayScaffold(
            contentVisible = true,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                HomeAppBar(
                    scrollBehavior = scrollBehavior,
                    homeNavController = navHostController,
                    isBusy = isScanningFolders,
                    activeLayout = viewState.homeLayout,
                    onScanCancel = {
                        scannerViewModel.viewModelScope.launch {
                            scannerViewModel.cancelScanFolders(context)
                            Toast.makeText(context, "Cancelled scan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSources = {
                        navHostController.navigate(Destination.Sources.route)
                    },
                    onSettings = {
                        navHostController.navigate(Destination.Settings.route)
                    },
                    onChangeLayout = viewModel::setHomeLayout,
                )
            },
            bottomBar = {
                NowPlayingView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.16F),
                ) {
                    navHostController.navigate(Destination.Player.useRoute(it))
                }
            },
        ) {

            val onBook: (BookProgressWithBookAndChapters) -> Unit = {
                val r = Destination.Player.useRoute(it.book.bookId, 0)
                logcat { "Open $r" }
                navHostController.navigate(r)
            }

            BooksList(
                viewState,
                modifier = Modifier.fillMaxSize(),
                onSortMenuChange = viewModel::setSortFilter,
                onSearchMenuEnabled = viewModel::setSearchEnabled,
                onSearchMenuChange = viewModel::setSearch,
                onFilterListVisible = viewModel::setFilterListVisible,
                onFilterListEnabled = viewModel::setFilterListEnabled,
                onFilterListAddSelection = viewModel::addFilterListSelection,
                onFilterListDeleteSelection = viewModel::deleteFilterListSelection,
                onBook,
            )
        }

        DisposableEffect(nowPlayingViewModel) {
            nowPlayingViewModel.setActive(true)

            onDispose {
                nowPlayingViewModel.setActive(false)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BooksList(
    viewState: HomeViewState,
    modifier: Modifier,
    onSortMenuChange: OnSortMenuChange<HomeSortType>,
    onSearchMenuEnabled: OnFilterEnabled,
    onSearchMenuChange: OnFilterChange,
    onFilterListVisible: OnFilterVisible,
    onFilterListEnabled: OnFilterEnabled,
    onFilterListAddSelection: OnAdd<String>,
    onFilterListDeleteSelection: OnDelete<String>,
    onBook: (BookProgressWithBookAndChapters) -> Unit,
) {

    val cellCount = if (viewState.isHomeLayoutGrid) gridColumnCount() else 1
    val isLandscape = LocalWindowSize.current.isLandscape
    val state: LazyGridState = rememberLazyGridState()
    val books = viewState.books() ?: emptyList()

    var menuExpandedIndex by remember { mutableIntStateOf(-1) }
    val setMenuExpandedIndex: (index: Int) -> Unit = { menuExpandedIndex = it }

    val bookContextMenuConfig = BookContextMenuConfig(
        isRestoreBookEnabled = false,
        isVisibilityChangeEnabled = true,
        onNavToRestore = {},
        onSetBookCategory = { _, _ -> },
        onBookVisibilityChange = { _, _ -> },
    )

    Column(modifier = Modifier.fillMaxWidth()) {

        FilterBar(
            sortMenuOptions = viewState.sortMenuOptions,
            onSortMenuChange = onSortMenuChange,
            filterMenuOptions = viewState.filterMenuOptions,
            onSearchMenuEnabled = onSearchMenuEnabled,
            onSearchMenuChange = onSearchMenuChange,
            filterListOptions = viewState.filterListOptions,
            onFilterListVisible = onFilterListVisible,
            onFilterListEnabled = onFilterListEnabled,
            onFilterListAddSelection = onFilterListAddSelection,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
        )

        ActiveFilterListOptions(
            filterListOptions = viewState.filterListOptions,
            onFilterListDeleteSelection = onFilterListDeleteSelection,
            onFilterListEnabled = onFilterListEnabled,
        )

        LazyVerticalGrid(
            state = state,
            columns = GridCells.Fixed(cellCount),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier,
        ) {

            items(books.size) { index ->
                val cellModifier = remember(index) {
                    Modifier
                        .alpha(if (books[index].book.isActive) 1F else 0.25F)
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onBook(books[index]) },
                            onLongClick = { setMenuExpandedIndex(index) },
                            onDoubleClick = {}
                        )
                }

                BookCell(
                    homeLayout = viewState.homeLayout,
                    bookProgressWithChapters = books[index],
                    config = bookContextMenuConfig,
                    isMenuExpanded = (index == menuExpandedIndex),
                    setMenuExpanded = { isExpanded ->
                        if (!isExpanded)
                            setMenuExpandedIndex(-1)
                        else
                            setMenuExpandedIndex(index)
                    },
                    modifier = cellModifier,
                )
            }

            if (!isLandscape) {
                items(cellCount) {
                    SpacerSystemBarsBottomPadding()
                }
            }
        }
    }
}

@ExperimentalLayoutApi
@Composable
fun ActiveFilterListOptions(
    filterListOptions: FilterListOptions<String>,
    onFilterListDeleteSelection: OnDelete<String>,
    onFilterListEnabled: OnFilterEnabled,
) {
    if (filterListOptions.isFilterEnabled && filterListOptions.selection.isNotEmpty()) {
        HorizontalDivider()

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {

            filterListOptions.selection.forEach {
                FilterChip(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    selected = false,
                    onClick = {},
                    label = { Text(text = it) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = null,
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                .clickable {
                                    onFilterListDeleteSelection(it)

                                    if ((filterListOptions.selection - it).isEmpty()) {
                                        onFilterListEnabled(false)
                                    }
                                },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun gridColumnCount(approxDp: Dp = 200.dp): Int {
    val displayMetrics = LocalContext.current.resources.displayMetrics
    val widthPx = displayMetrics.widthPixels.toFloat()
    val desiredPx = with(LocalDensity.current) {
        approxDp.toPx()
    }
    val columns = (widthPx / desiredPx).roundToInt()
    return columns.coerceAtLeast(2)
}
