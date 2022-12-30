package com.dotslashlabs.sensay.common

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Bundle
import androidx.media3.session.SessionCommand
import logcat.logcat

enum class AudioEffectCommands(private val commandName: String) {
    VOLUME_BOOST("Loudness Enhancer"),
    BASS_BOOST("Dynamic Bass Boost"),
    REVERB("Insert Preset Reverb");

    fun toCommand() = SessionCommand(commandName, Bundle())

    companion object {
        const val CUSTOM_ACTION_ARG_ENABLED = "isEnabled"
        const val RESULT_ARG_ERROR = "error"

        val commands = values().map { it.toCommand() }

        fun resolve(commandName: String): AudioEffectCommands? = values().firstOrNull {
            it.commandName == commandName
        }

        fun isEnabled(args: Bundle) =
            args.getBoolean(ExtraSessionCommands.CUSTOM_ACTION_ARG_ENABLED, false)

        fun toAudioEffect(
            audioSessionId: Int,
            customCommand: SessionCommand,
            isEnabled: Boolean = false,
            priority: Int = 0,
        ): AudioEffect? {

            return when (customCommand.customAction) {
                VOLUME_BOOST.commandName -> LoudnessEnhancer(audioSessionId).apply {
                    if (!isEnabled) return@apply

                    setTargetGain(800)
                }
                BASS_BOOST.commandName -> BassBoost(priority, audioSessionId).apply {
                    if (!isEnabled) return@apply

                    if (strengthSupported) {
                        logcat { "EQ band=bass previousLevel=$roundedStrength" }
                         setStrength(300)
                    }
                }
                REVERB.commandName -> PresetReverb(priority, audioSessionId).apply {
                    if (!isEnabled) return@apply

                    preset = PresetReverb.PRESET_SMALLROOM
                }
                else -> return null
            }.apply { enabled = isEnabled }
        }
    }
}
