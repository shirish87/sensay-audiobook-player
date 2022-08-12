package com.dotslashlabs.sensay

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dotslashlabs.sensay.service.PlaybackService
import com.dotslashlabs.sensay.ui.app.SensayApp
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity(), ActivityBridge {
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SensayApp(this)
        }
    }

    override fun onStart() {
        super.onStart()

        initializeController()
    }

    override fun onStop() {
        super.onStop()

        releaseController()
    }

    private fun initializeController() {
        controllerFuture = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlaybackService::class.java))
        ).buildAsync()
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    override val mediaController: () -> MediaController? = {
        if (controllerFuture.isDone)
            controllerFuture.get()
        else null
    }
}
