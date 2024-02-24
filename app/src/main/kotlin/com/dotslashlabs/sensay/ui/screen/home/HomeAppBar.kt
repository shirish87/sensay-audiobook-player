package com.dotslashlabs.sensay.ui.screen.home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dotslashlabs.sensay.ui.common.TopProgressAppBar
import com.dotslashlabs.sensay.ui.screen.settings.OnNavToSettings
import com.dotslashlabs.sensay.ui.screen.sources.OnNavToSources
import compose.icons.MaterialIcons
import compose.icons.materialicons.Book
import compose.icons.materialicons.GridView
import compose.icons.materialicons.Settings
import compose.icons.materialicons.ViewList
import config.HomeLayout
import logcat.logcat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAppBar(
    homeNavController: NavHostController,
    isBusy: Boolean,
    activeLayout: HomeLayout,
    @Suppress("UNUSED_PARAMETER") onScanCancel: () -> Unit,
    onSources: OnNavToSources,
    onSettings: OnNavToSettings,
    onChangeLayout: (layout: HomeLayout) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {

    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    logcat("HomeAppBar") { "navBackStackEntry: $navBackStackEntry" }

    TopProgressAppBar(
        scrollBehavior = scrollBehavior,
        isBusy = isBusy,
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
                        HomeLayout.LIST -> MaterialIcons.GridView
                        HomeLayout.GRID -> MaterialIcons.ViewList
                    },
                    contentDescription = "",
                )
            }

            IconButton(onClick = onSources) {
                Icon(
                    imageVector = MaterialIcons.Book,
                    contentDescription = "",
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = MaterialIcons.Settings,
                    contentDescription = "",
                )
            }
        },
    )
}
