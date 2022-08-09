package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    isBusy: Boolean,
    activeLayout: Layout,
    onAdd: () -> Unit,
    onChangeLayout: (layout: Layout) -> Unit,
    onSettings: () -> Unit,
) {
    SmallTopAppBar(
        title = { Text("Sensay") },
        actions = {
            if (isBusy) {
                IconButton(onClick = {}) {
                    CircularProgressIndicator()
                }
            }
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = "",
                )
            }
            IconButton(onClick = {
                onChangeLayout(
                    when (activeLayout) {
                        Layout.LIST -> Layout.GRID
                        Layout.GRID -> Layout.LIST
                    }
                )
            }) {
                Icon(
                    imageVector = when (activeLayout) {
                        Layout.LIST -> Icons.Outlined.GridView
                        Layout.GRID -> Icons.Outlined.ViewList
                    },
                    contentDescription = "",
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "",
                )
            }
        }
    )
}
