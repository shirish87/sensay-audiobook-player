package com.dotslashlabs.sensay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.dotslashlabs.sensay.ui.SensayApp
import com.dotslashlabs.sensay.ui.ServiceConnection
import com.dotslashlabs.sensay.util.createDevicePostureFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serviceConnection: ServiceConnection


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val devicePostureFlow = createDevicePostureFlow()

        setContent {
            val windowSize = calculateWindowSizeClass(this)
            val devicePosture by devicePostureFlow.collectAsState()

            SensayApp(windowSize, devicePosture, intent.action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceConnection.release()
    }
}
