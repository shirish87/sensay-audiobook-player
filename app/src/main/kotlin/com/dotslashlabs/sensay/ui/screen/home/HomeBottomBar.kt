package com.dotslashlabs.sensay.ui.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dotslashlabs.sensay.ui.screen.Destination

@Composable
fun HomeBottomBar(
    homeNavController: NavHostController,
    childDestinations: Collection<Destination>,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier) {
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
                label = { Text(text = dest.route) },
                selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                onClick = {
                    homeNavController.navigate(dest.route) {
                        popUpTo(homeNavController.graph.findStartDestination().id) {
                            saveState = true
                        }

                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
