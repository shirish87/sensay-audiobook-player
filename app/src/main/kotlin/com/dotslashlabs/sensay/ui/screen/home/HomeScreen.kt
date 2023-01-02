package com.dotslashlabs.sensay.ui.screen.home

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.nowplaying.NowPlayingView
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.launch

object HomeScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = HomeContent(destination, navHostController, backStackEntry)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
)
fun HomeContent(
    @Suppress("UNUSED_PARAMETER") destination: Destination,
    @Suppress("UNUSED_PARAMETER") navHostController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
) {

    val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
    val state by appViewModel.collectAsState()

    val homeNavController = rememberAnimatedNavController()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isTopBarCollapsed =
        (scrollBehavior.state.heightOffset == scrollBehavior.state.heightOffsetLimit)

    val context: Context = LocalContext.current

    SensayFrame {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                HomeAppBar(
                    scrollBehavior = scrollBehavior,
                    homeNavController = homeNavController,
                    isBusy = state.isScanningFolders,
                    activeLayout = state.homeLayout,
                    onChangeLayout = {
                        appViewModel.setHomeLayout(it)
                    },
                    onSources = {
                        navHostController.navigate(Destination.Sources.route)
                    },
                    onScanCancel = {
                        appViewModel.viewModelScope.launch {
                            appViewModel.cancelScanFolders(context)
                            Toast.makeText(context, "Cancelled scan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSettings = {
                        navHostController.navigate(Destination.Settings.route)
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                        .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.5F))
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = true),
                ) {

                    val nowPlayingView = @Composable {
                        NowPlayingView(
                            backStackEntry,
                            navHostController::navigate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                        )
                    }

                    if (state.useLandscapeLayout) {
                        AnimatedVisibility(visible = isTopBarCollapsed) {
                            nowPlayingView()
                        }
                    } else {
                        nowPlayingView()
                    }

                    AnimatedVisibility(visible = !isTopBarCollapsed) {
                        HomeBottomBar(
                            homeNavController,
                            destination.children,
                            useLandscapeLayout = state.useLandscapeLayout,
                        )
                    }
                }
            },
        ) { innerPadding ->

            val startDestination = destination.defaultChild?.route ?: return@Scaffold

            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = computeContentPadding(
                innerPadding,
                isTopBarCollapsed,
                layoutDirection,
            )

            AnimatedNavHost(
                homeNavController,
                startDestination = startDestination,
                modifier = Modifier.padding(contentPadding),
            ) {
                destination.children.map { dest ->
                    dest.screen?.navGraph(dest, this, navHostController)
                }
            }
        }
    }
}

@Composable
fun computeContentPadding(
    innerPadding: PaddingValues,
    isTopBarCollapsed: Boolean,
    layoutDirection: LayoutDirection,
) = remember(innerPadding, isTopBarCollapsed, layoutDirection) {
    if (isTopBarCollapsed) {
        PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
        )
    } else {
        PaddingValues(
            top = innerPadding.calculateTopPadding(),
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
        )
    }
}
