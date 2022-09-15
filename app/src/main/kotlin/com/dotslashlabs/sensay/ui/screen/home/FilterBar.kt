package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

typealias OnFilterChange = (filter: String) -> Unit
typealias OnFilterEnabled = (enabled: Boolean) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <SortMenuType> FilterBar(
    sortMenuItems: Collection<Pair<SortMenuType, ImageVector>>,
    sortMenuDefaults: SortFilter<SortMenuType>,
    onSortMenuChange: OnSortMenuChange<SortMenuType>,
    isFilterEnabled: Boolean,
    onFilterEnabled: OnFilterEnabled,
    filter: String,
    onFilterChange: OnFilterChange,
    filterLabel: String,
    modifier: Modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
) {

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier,
    ) {

        item {
            SortMenu(
                menuItems = sortMenuItems,
                defaults = sortMenuDefaults,
                onSortMenuChange = onSortMenuChange,
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
