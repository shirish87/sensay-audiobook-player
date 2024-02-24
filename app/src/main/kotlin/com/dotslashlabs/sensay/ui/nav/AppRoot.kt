package com.dotslashlabs.sensay.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavTree() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.Root.defaultChild.route,
    ) {
        Destination.Root.children.map {
            it.screen?.navGraph(it, this, navController)
        }
    }
}
