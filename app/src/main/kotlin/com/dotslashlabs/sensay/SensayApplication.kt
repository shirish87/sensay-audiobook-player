package com.dotslashlabs.sensay

import android.app.Application
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority

@HiltAndroidApp
class SensayApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
        // Mavericks.initialize(this, MavericksViewModelConfigFactory(debugMode = false))
        Mavericks.initialize(this)
    }
}
