package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import compose.icons.MaterialIcons
import compose.icons.materialicons.Clear
import compose.icons.materialicons.FilterList
import compose.icons.materialicons.Search

typealias OnFilterChange = (filter: String) -> Unit
typealias OnFilterEnabled = (enabled: Boolean) -> Unit
typealias OnFilterVisible = (visible: Boolean) -> Unit

data class FilterMenuOptions(
    val isFilterEnabled: Boolean,
    val filter: String,
    val filterLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <SortMenuType> FilterBar(
    sortMenuOptions: SortMenuOptions<SortMenuType>,
    onSortMenuChange: OnSortMenuChange<SortMenuType>,
    filterMenuOptions: FilterMenuOptions,
    onSearchMenuEnabled: OnFilterEnabled,
    onSearchMenuChange: OnFilterChange,
    filterListOptions: FilterListOptions<String>,
    onFilterListVisible: OnFilterVisible,
    onFilterListEnabled: OnFilterEnabled,
    onFilterListAddSelection: OnAdd<String>,
    modifier: Modifier = Modifier,
) {

    val (
        isFilterEnabled,
        filter,
        filterLabel,
    ) = filterMenuOptions

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier,
    ) {

        item {
            FilterChip(
                shape = MaterialTheme.shapes.small,
                selected = isFilterEnabled,
                onClick = { onSearchMenuEnabled(!isFilterEnabled) },
                label = { Text("Search") },
                leadingIcon = {
                    Icon(
                        MaterialIcons.Search,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }

        item {
            FilterChip(
                shape = MaterialTheme.shapes.small,
                selected = filterListOptions.isFilterEnabled,
                onClick = {
                    if (filterListOptions.selection.isNotEmpty() && !filterListOptions.isFilterVisible) {
                        // re-display selection view for additional selections
                        onFilterListVisible(true)
                    } else {
                        onFilterListEnabled(!filterListOptions.isFilterEnabled)
                    }
                },
                label = { Text(filterListOptions.filterLabel) },
                leadingIcon = {
                    Icon(
                        MaterialIcons.FilterList,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                trailingIcon = {
                    if (filterListOptions.selection.isNotEmpty()) {
                        Icon(
                            imageVector = MaterialIcons.Clear,
                            contentDescription = null,
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                .clickable { onFilterListEnabled(false) },
                        )
                    }
                },
            )
        }

        item {
            SortMenu(
                sortMenuOptions = sortMenuOptions,
                onSortMenuChange = onSortMenuChange,
            )
        }
    }

    if (isFilterEnabled) {
        val focusRequester = remember { FocusRequester() }

        Row {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .focusRequester(focusRequester),
                label = { Text(filterLabel) },
                value = filter,
                onValueChange = onSearchMenuChange,
                singleLine = true,
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        Icon(
                            MaterialIcons.Clear,
                            contentDescription = null,
                            modifier = Modifier.clickable { onSearchMenuChange("") },
                        )
                    }
                },
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    FilterList(
        filterListOptions = filterListOptions,
        onFilterListVisible = onFilterListVisible,
        onFilterListAdd = onFilterListAddSelection,
    )
}
