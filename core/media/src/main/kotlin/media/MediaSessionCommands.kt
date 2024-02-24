package media

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.session.SessionCommand

enum class MediaSessionCommands(private val commandName: String) {
    RESOLVE_MEDIA("resolveMedia");

    fun toCommand(bundle: Bundle = Bundle.EMPTY) = SessionCommand(commandName, bundle)

    companion object {
        private const val KEY_MEDIA_URI = "mediaUri"
        const val KEY_BOOK_ID = "bookId"
        const val KEY_CHAPTER_INDEX = "chapterIndex"
        const val RESULT_ARG_PLAYER_STATE = "playerState"
        const val RESULT_ARG_PLAYABLE_MEDIA_ITEM = "playableMediaItem"
        const val RESULT_ARG_ERROR = "error"

        val commands = values().map { it.toCommand() }

        fun resolve(commandName: String): MediaSessionCommands? = values().firstOrNull {
            it.commandName == commandName
        }

        @Suppress("DEPRECATION")
        fun getMediaUri(args: Bundle) = args.getParcelable<Uri>(MediaSessionCommands.KEY_MEDIA_URI)

        fun getBookId(args: Bundle) = args.getLong(MediaSessionCommands.KEY_BOOK_ID)

        fun getChapterIndex(args: Bundle) = args.getInt(MediaSessionCommands.KEY_CHAPTER_INDEX)
    }
}
