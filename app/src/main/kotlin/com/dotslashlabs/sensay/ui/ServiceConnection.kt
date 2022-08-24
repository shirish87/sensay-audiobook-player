package com.dotslashlabs.sensay.ui

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dotslashlabs.sensay.common.PlayerHolder
import com.dotslashlabs.sensay.common.SensayPlayer
import com.dotslashlabs.sensay.service.PlaybackService
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import logcat.logcat


class ServiceConnection(
    context: Context,
    private val playerHolder: PlayerHolder,
) {

    private var error: Throwable? = null

    init {
        logcat { "launch" }
        release()

        Futures.addCallback(
            MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            ).buildAsync(),
            object : FutureCallback<MediaController> {

                override fun onSuccess(result: MediaController?) {
                    logcat { "launch: onSuccess=$result" }
                    playerHolder.load(result?.let { SensayPlayer(it) })
                }

                override fun onFailure(t: Throwable) {
                    logcat { "launch: onFailure=${t.message}" }
                    error = t
                    release()
                }
            },
            context.mainExecutor,
        )
    }

    fun release() {
        logcat { "release" }

        playerHolder.clear()
    }
}
