package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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

data class FilterMenuOptions(
    val isFilterEnabled: Boolean,
    val onFilterEnabled: OnFilterEnabled,
    val filter: String,
    val onFilterChange: OnFilterChange,
    val filterLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <SortMenuType> FilterBar(
    sortMenuOptions: SortMenuOptions<SortMenuType>,
    filterMenuOptions: FilterMenuOptions,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 40.dp),
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
                label = { Text("Authors") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
        item {
            SortMenu(
                sortMenuOptions = sortMenuOptions,
            )
        }
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
                            Icons.Default.Clear,
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
}
