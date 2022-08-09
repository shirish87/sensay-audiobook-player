package com.dotslashlabs.sensay.ui.screen.home

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

object HomeScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = HomeContent(destination, activityBridge, navHostController, backStackEntry)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
)
fun HomeContent(
    @Suppress("UNUSED_PARAMETER") destination: Destination,
    @Suppress("UNUSED_PARAMETER") activityBridge: ActivityBridge,
    @Suppress("UNUSED_PARAMETER") navHostController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
) {
    val viewModel: HomeViewModel = mavericksViewModel(backStackEntry)
    val state by viewModel.collectAsState()
    val homeNavController = rememberAnimatedNavController()

    SensayFrame {
        Scaffold(
            topBar = {
                HomeAppBar(
                    isBusy = false,
                    activeLayout = state.activeLayout,
                    onChangeLayout = {
                        viewModel.setActiveLayout(it)
                    },
                    onAdd = {
                        navHostController.navigate(Destination.Sources.route)
                    },
                    onSettings = {
                        navHostController.navigate(Destination.Settings.route)
                    },
                )
            },
            bottomBar = {
                HomeBottomBar(homeNavController, destination.children)
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                val startDestination = destination.defaultChild?.route ?: return@Column

                AnimatedNavHost(
                    homeNavController,
                    startDestination = startDestination,
                ) {
                    destination.children.map { dest ->
                        dest.screen?.navGraph(dest, this, navHostController, activityBridge)
                    }
                }
            }
        }
    }
}
