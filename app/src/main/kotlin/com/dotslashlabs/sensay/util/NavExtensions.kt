package com.dotslashlabs.sensay.util

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
fun NavBackStackEntry.isLifecycleResumed() =
    lifecycle.currentState == Lifecycle.State.RESUMED
