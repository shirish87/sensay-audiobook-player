package com.dotslashlabs.sensay.ui.screen.home

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
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
    val context: Context = LocalContext.current

    SensayFrame {
        Scaffold(
            topBar = {
                HomeAppBar(
                    isBusy = state.isScanningFolders,
                    activeLayout = state.homeLayout,
                    onChangeLayout = {
                        appViewModel.setHomeLayout(it)
                    },
                    onSources = {
                        navHostController.navigate(Destination.Sources.route)
                    },
                    onScanCancel = {
                        appViewModel.cancelScanFolders()

                        appViewModel.viewModelScope.launch {
                            Toast.makeText(context, "Cancelled scan", Toast.LENGTH_SHORT).show()
                        }
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

                ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                    val (listRef, nowPlayingRef) = createRefs()

                    AnimatedNavHost(
                        homeNavController,
                        startDestination = startDestination,
                        modifier = Modifier.constrainAs(listRef) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                            bottom.linkTo(nowPlayingRef.top)
                        }
                    ) {
                        destination.children.map { dest ->
                            dest.screen?.navGraph(dest, this, navHostController)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .constrainAs(nowPlayingRef) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom)
                            },
                    ) {
                        NowPlayingView(navHostController, backStackEntry)
                    }
                }
            }
        }
    }
}
