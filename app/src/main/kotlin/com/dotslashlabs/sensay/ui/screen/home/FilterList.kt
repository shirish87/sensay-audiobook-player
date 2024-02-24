package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

typealias OnAdd<T> = (item: T) -> Unit
typealias OnDelete<T> = (item: T) -> Unit

data class FilterListOptions<T>(
    val isFilterEnabled: Boolean,
//    val onFilterEnabled: OnFilterEnabled,
    val isFilterVisible: Boolean,
//    val onFilterVisible: OnFilterVisible,
    val items: Collection<T>,
    val selection: Collection<T>,
//    val onAdd: OnAdd<T>,
//    val onDelete: OnDelete<T>,
    val filterLabel: String,
)

@ExperimentalLayoutApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterList(
    filterListOptions: FilterListOptions<T>,
    onFilterListVisible: OnFilterVisible,
    onFilterListAdd: OnAdd<T>,
) {

    if (!filterListOptions.isFilterVisible) return

    val (
        _,
        _,
        items,
        selection,
    ) = filterListOptions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.25F)
            .verticalScroll(rememberScrollState()),
    ) {

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            // verticalAlignment = Alignment.Top,
        ) {
            items.filter { !selection.contains(it) }.forEach {
                FilterChip(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    selected = false,
                    onClick = {
                        onFilterListAdd(it)
                        onFilterListVisible(false)
                    },
                    label = { Text(text = "$it") },
                )
            }
        }
    }
}
