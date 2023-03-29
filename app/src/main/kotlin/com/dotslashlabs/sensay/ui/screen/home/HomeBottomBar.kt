package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dotslashlabs.sensay.ui.screen.Destination
import logcat.logcat

@Composable
fun HomeBottomBar(
    homeNavController: NavHostController,
    childDestinations: Collection<Destination>,
    useLandscapeLayout: Boolean,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
) {
    NavigationBar(
        modifier = modifier,
        tonalElevation = tonalElevation,
    ) {
        val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        childDestinations.map { dest ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (dest.screen) {
                            CurrentScreen -> Icons.Outlined.Headphones
                            else -> Icons.Outlined.LibraryBooks
                        },
                        contentDescription = null,
                    )
                },
                label = if (useLandscapeLayout) null else ({ Text(text = dest.screen.toString()) }),
                selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                onClick = {
                    logcat { "NavigationBarItem: ${dest.route}" }
                    homeNavController.navigate(dest.route) {
                        homeNavController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
