package com.dotslashlabs.sensay.ui.screen.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

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

    val context: Context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)

        viewModel.addAudiobookFolders(setOf(uri))
            .invokeOnCompletion {
                if (it != null) return@invokeOnCompletion

                viewModel.scanFolders(context)
            }
    }

    SensayFrame {
        Scaffold(
            topBar = {
                HomeAppBar(
                    isBusy = state.isScanningFolders,
                    activeLayout = state.homeLayout,
                    onChangeLayout = {
                        viewModel.setHomeLayout(it)
                    },
                    onScan = {
                        launcher.launch(null)
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
