package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SortType(val displayName: String) {
    CREATED_AT("Created"),
    UPDATED_AT("Updated"),
    NAME("Name"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortMenu() {
    var expanded: Boolean by remember { mutableStateOf(false) }
    var sortType: SortType by remember { mutableStateOf(SortType.CREATED_AT) }

    FilterChip(
        shape = MaterialTheme.shapes.small,
        selected = sortType != SortType.CREATED_AT,
        onClick = { expanded = true },
        label = { Text("Sort by ${sortType.displayName}") },
        leadingIcon = {
            Icon(
                Icons.Outlined.Sort,
                contentDescription = null,
                Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )

    val setSortType: (type: SortType) -> Unit = { type ->
        sortType = type
        expanded = false
    }

    DropdownMenu(
        modifier = Modifier,
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {

        SortType.values().map { type ->
            DropdownMenuItem(
                text = { Text(type.displayName) },
                onClick = { setSortType(type) },
                leadingIcon = {
                    if (type == sortType) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar() {

    var selectedTabIndex by remember { mutableStateOf(0) }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
        modifier = Modifier.heightIn(min = 56.dp)
    ) {

        item {
            SortMenu()
        }
        item {
            FilterChip(
                shape = MaterialTheme.shapes.small,
                selected = (selectedTabIndex == 1),
                onClick = { selectedTabIndex = 1 },
                label = { Text("Filter") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
        item {
            FilterChip(
                shape = MaterialTheme.shapes.small,
                selected = (selectedTabIndex == 2),
                onClick = { selectedTabIndex = 2 },
                label = { Text("Search") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}
