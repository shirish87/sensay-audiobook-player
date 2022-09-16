package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow


typealias OnAdd<T> = (item: T) -> Unit
typealias OnDelete<T> = (item: T) -> Unit

data class FilterListOptions<T>(
    val isFilterEnabled: Boolean,
    val onFilterEnabled: OnFilterEnabled,
    val items: Collection<T>,
    val selection: Collection<T>,
    val onAdd: OnAdd<T>,
    val onDelete: OnDelete<T>,
    val filterLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterList(
    filterListOptions: FilterListOptions<T>,
) {

    val (
        _,
        _,
        items,
        selection,
        onAdd,
        onDelete,
        filterLabel,
    ) = filterListOptions

    var expanded by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {

        val filteredItems = items.filter {
            "$it".contains(inputText, ignoreCase = true) && !selection.contains(it)
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
            mainAxisSpacing = 6.dp,
        ) {

            selection.forEach {
                InputChip(
                    selected = false,
                    onClick = { /*TODO*/ },
                    label = { Text(text = "$it") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = null,
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                .clickable { onDelete(it) },
                        )
                    },
                )
            }

            val focusRequester = remember { FocusRequester() }

            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    expanded = if (filteredItems.firstOrNull()?.let { f ->
                            "$f".contentEquals(it, ignoreCase = true)
                        } == true) {
                        false
                    } else {
                        filteredItems.isNotEmpty()
                    }
                },
                label = { Text(filterLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val match = filteredItems.firstOrNull {
                            "$it".contentEquals(inputText, ignoreCase = true)
                        }

                        if (match != null) {
                            onAdd(match)
                            inputText = ""
                            expanded = false
                        }
                    },
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .focusRequester(focusRequester),
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        if (filteredItems.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {

                filteredItems.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(text = "$selectionOption") },
                        onClick = {
                            onAdd(selectionOption)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
