package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dotslashlabs.sensay.ui.screen.Destination
import config.HomeLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    homeNavController: NavHostController,
    isBusy: Boolean,
    activeLayout: HomeLayout,
    @Suppress("UNUSED_PARAMETER") onScanCancel: () -> Unit,
    onSources: () -> Unit,
    onSettings: () -> Unit,
    onChangeLayout: (layout: HomeLayout) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {

    Box {
        val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
        val isLibraryRoute =
            (navBackStackEntry?.destination?.route == Destination.Home.Library.route)

        TopAppBar(
            scrollBehavior = scrollBehavior,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
            title = { Text("Sensay") },
            actions = {
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
                if (isLibraryRoute) {
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
            }
        )

        if (isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            )
        }
    }
}
