package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.MaterialIcons
import compose.icons.materialicons.Check
import compose.icons.materialicons.Clear
import compose.icons.materialicons.North
import compose.icons.materialicons.Sort
import compose.icons.materialicons.South

typealias SortFilter<T> = Pair<T, Boolean>
typealias OnSortMenuChange<T> = (sortFilter: SortFilter<T>) -> Unit

data class SortMenuOptions<SortMenuType>(
    val sortMenuItems: Collection<Pair<SortMenuType, ImageVector>>,
    val sortMenuDefaults: SortFilter<SortMenuType>,
    // val onSortMenuChange: OnSortMenuChange<SortMenuType>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortMenu(
    sortMenuOptions: SortMenuOptions<T>,
    onSortMenuChange: OnSortMenuChange<T>,
    labelPrefix: String = "",
) {

    val (menuItems, defaults) = sortMenuOptions

    var sortFilter: SortFilter<T> by remember { mutableStateOf(defaults) }
    var expanded: Boolean by remember { mutableStateOf(false) }

    val isAscending = sortFilter.second
    val isDefault = (sortFilter == defaults)

    val setSortFilter: (SortFilter<T>) -> Unit = {
        onSortMenuChange(it)
        sortFilter = it
        expanded = false
    }

    val label = listOf(labelPrefix, "${sortFilter.first}")
        .filter { it.isNotBlank() }
        .joinToString(" ")

    FilterChip(
        shape = MaterialTheme.shapes.small,
        selected = !isDefault,
        onClick = { expanded = true },
        label = { Text(label) },
        leadingIcon = {
            val modifier = if (!isDefault)
                Modifier.clickable { sortFilter = defaults }
            else Modifier

            Icon(
                imageVector = if (!isDefault)
                    MaterialIcons.Clear
                else MaterialIcons.Sort,
                contentDescription = null,
                modifier = Modifier
                    .size(FilterChipDefaults.IconSize)
                    .then(modifier)
                    .clickable { setSortFilter(sortMenuOptions.sortMenuDefaults) },
            )
        },
        trailingIcon = {
            Icon(
                imageVector = if (isAscending)
                    MaterialIcons.South
                else MaterialIcons.North,
                contentDescription = null,
                modifier = Modifier
                    .size(FilterChipDefaults.IconSize)
                    .clickable { setSortFilter(sortFilter.copy(second = !isAscending)) },
            )
        },
    )

    DropdownMenu(
        modifier = Modifier,
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {

        menuItems.map { type ->
            DropdownMenuItem(
                text = { Text("${type.first}") },
                onClick = { setSortFilter(sortFilter.copy(first = type.first)) },
                leadingIcon = {
                    Icon(
                        type.second,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (type.first == sortFilter.first) {
                        Icon(
                            MaterialIcons.Check,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    }
}
