package com.dotslashlabs.sensay.ui.screen.home.current

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel

object CurrentScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {
        val parentBackStackEntryEntry = remember {
            navHostController.getBackStackEntry(destination.parentRoute())
        }

        val viewModel: HomeViewModel = mavericksViewModel(parentBackStackEntryEntry)
        val state by viewModel.collectAsState()

        SensayFrame(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${destination.route} ${state.activeLayout}",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { navHostController.navigate(Destination.Book.Player.useRoute(1L)) },
            )
        }
    }
}
