package com.dotslashlabs.sensay.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.dotslashlabs.sensay.ui.nav.AppScreen
import com.dotslashlabs.sensay.ui.nav.Destination


typealias OnNavToSettings = () -> Unit

object SettingsScreen : AppScreen {

    @UnstableApi
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = "github.com/shirish87",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { navHostController.popBackStack() },
            )
        }
    }
}
