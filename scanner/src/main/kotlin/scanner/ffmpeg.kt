package scanner

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.arthenica.ffmpegkit.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.asLog
import logcat.logcat

class FfmpegException(message: String) : Exception(message)

suspend fun ffprobe(input: Uri, context: Context, command: List<String>): String? {
    FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
    val fullCommand = fullCommand(input, context, command) ?: return null
    return suspendCancellableCoroutine { cont ->
        val probeSession = FFprobeKit.executeWithArgumentsAsync(
            fullCommand.toTypedArray(),
        ) { session ->
            when (session.state) {
                SessionState.COMPLETED -> {
                    cont.resume(session.output)
                }
                SessionState.FAILED -> {
                    cont.resume(null)
                }
                else -> {}
            }
        }
        cont.invokeOnCancellation { probeSession.cancel() }
    }
}

suspend fun ffmpeg(input: Uri, context: Context, command: List<String>): String? {
    FFmpegKitConfig.setLogRedirectionStrategy(LogRedirectionStrategy.NEVER_PRINT_LOGS)
    val fullCommand = fullCommand(input, context, command) ?: return null
    return suspendCancellableCoroutine { cont ->
        val probeSession = FFmpegKit.executeWithArgumentsAsync(
            fullCommand.toTypedArray(),
        ) { session ->
            when (session.state) {
                SessionState.COMPLETED -> {
                    cont.resume(
                        """
                        |$fullCommand
                        |${session.output}
                        """.trimMargin()
                    )
                }
                SessionState.FAILED -> {
                    cont.resumeWithException(FfmpegException(session.failStackTrace))
                }
                else -> {}
            }
        }
        cont.invokeOnCancellation { probeSession.cancel() }
    }
}

private fun fullCommand(input: Uri, context: Context, command: List<String>): List<String>? {
    val mappedInput = if (input.scheme == "content") {
        try {
            FFmpegKitConfig.getSafParameterForRead(context, input)
        } catch (e: Exception) {
            logcat("ffmpeg") { "Could not get saf parameter for $input: ${e.asLog()}" }
            return null
        }
    } else {
        input.toFile().absolutePath
    }
    return listOf("-i", mappedInput) + command
}
