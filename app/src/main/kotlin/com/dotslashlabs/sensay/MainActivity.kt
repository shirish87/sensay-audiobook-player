package com.dotslashlabs.sensay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.dotslashlabs.sensay.service.PlaybackConnection
import com.dotslashlabs.sensay.ui.SensayApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackConnection: PlaybackConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SensayApp()
        }
    }

    override fun onStart() {
        super.onStart()

        playbackConnection.start()
    }

    override fun onStop() {
        super.onStop()

        playbackConnection.stop()
    }
}
