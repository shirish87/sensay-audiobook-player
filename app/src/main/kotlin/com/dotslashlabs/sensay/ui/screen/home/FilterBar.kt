package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

typealias OnFilterChange = (filter: String) -> Unit
typealias OnFilterEnabled = (enabled: Boolean) -> Unit
typealias OnFilterVisible = (visible: Boolean) -> Unit

data class FilterMenuOptions(
    val isFilterEnabled: Boolean,
    val onFilterEnabled: OnFilterEnabled,
    val filter: String,
    val onFilterChange: OnFilterChange,
    val filterLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <SortMenuType> FilterBar(
    sortMenuOptions: SortMenuOptions<SortMenuType>,
    filterMenuOptions: FilterMenuOptions,
    filterListOptions: FilterListOptions<String>,
    modifier: Modifier = Modifier,
) {

    val (
        isFilterEnabled,
        onFilterEnabled,
        filter,
        onFilterChange,
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
                onClick = { onFilterEnabled(!isFilterEnabled) },
                label = { Text("Search") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
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
                        filterListOptions.onFilterVisible(true)
                    } else {
                        filterListOptions.onFilterEnabled(!filterListOptions.isFilterEnabled)
                    }
                },
                label = { Text(filterListOptions.filterLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                trailingIcon = {
                    if (filterListOptions.selection.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = null,
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                .clickable { filterListOptions.onFilterEnabled(false) },
                        )
                    }
                },
            )
        }

        item {
            SortMenu(
                sortMenuOptions = sortMenuOptions,
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
                onValueChange = onFilterChange,
                singleLine = true,
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = null,
                            modifier = Modifier.clickable { onFilterChange("") },
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
    )
}
