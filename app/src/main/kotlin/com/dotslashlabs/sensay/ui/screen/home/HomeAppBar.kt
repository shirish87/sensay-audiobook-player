package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import config.HomeLayout


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    isBusy: Boolean,
    activeLayout: HomeLayout,
    onSources: () -> Unit,
    onScanCancel: () -> Unit,
    onChangeLayout: (layout: HomeLayout) -> Unit,
    onSettings: () -> Unit,
) {
    SmallTopAppBar(
        title = { Text("Sensay") },
        actions = {
            if (isBusy) {
                IconButton(onClick = onScanCancel) {
                    CircularProgressIndicator()
                }
            }
            IconButton(onClick = {
                onChangeLayout(
                    when (activeLayout) {
                        HomeLayout.LIST -> HomeLayout.GRID
                        HomeLayout.GRID -> HomeLayout.LIST
                    }
                )
            }) {
                Icon(
                    imageVector = when (activeLayout) {
                        HomeLayout.LIST -> Icons.Outlined.GridView
                        HomeLayout.GRID -> Icons.Outlined.ViewList
                    },
                    contentDescription = "",
                )
            }
            IconButton(onClick = onSources) {
                Icon(
                    imageVector = Icons.Outlined.Book,
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
