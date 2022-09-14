package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <SortMenuType> FilterBar(
    sortMenuItems: Collection<Pair<SortMenuType, ImageVector>>,
    sortMenuDefaults: SortFilter<SortMenuType>,
    onSortMenuChange: OnSortMenuChange<SortMenuType>,
    modifier: Modifier = Modifier.heightIn(min = 40.dp),
) {

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                selected = false,
                onClick = {},
                label = { Text("Filter") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMenu() {

    var expanded: Boolean by remember { mutableStateOf(false) }

    FilledTonalIconToggleButton(
        checked = expanded,
        onCheckedChange = { expanded = it },
        shape = MaterialTheme.shapes.small,
    ) {
        if (expanded) {
            Icon(Icons.Filled.Search, contentDescription = null)
        } else {
            Icon(Icons.Outlined.Search, contentDescription = null)
        }
    }

    var text by rememberSaveable { mutableStateOf("") }

    DropdownMenu(
        modifier = Modifier.fillMaxWidth(),
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {

        DropdownMenuItem(
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Label") }
                )
            },
            onClick = {},
            leadingIcon = {},
        )
    }
}
