package com.dotslashlabs.sensay.common

import android.os.Bundle
import androidx.media3.session.SessionCommand

enum class ExtraSessionCommands(private val commandName: String) {
    SKIP_SILENCE("skipSilence");

    fun toCommand() = SessionCommand(commandName, Bundle())

    companion object {
        const val CUSTOM_ACTION_ARG_ENABLED = "isEnabled"

        val commands = values().map { it.toCommand() }

        fun resolve(commandName: String): ExtraSessionCommands? = values().firstOrNull {
            it.commandName == commandName
        }

        fun isEnabled(args: Bundle) = args.getBoolean(CUSTOM_ACTION_ARG_ENABLED, false)
    }
}
