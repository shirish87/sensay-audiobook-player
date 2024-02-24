package media

import android.os.Bundle
import androidx.media3.session.SessionCommand
import data.util.ContentDuration

enum class ExtraSessionCommands(private val commandName: String) {
    SKIP_SILENCE("skipSilence"),
    BOOKMARK_ADD("bookmarkAdd"),
    BOOKMARK_REMOVE("bookmarkRemove");

    fun toCommand() = SessionCommand(commandName, Bundle())

    companion object {
        const val CUSTOM_ACTION_ARG_ENABLED = "isEnabled"
        const val CUSTOM_ACTION_BOOKMARK_ID = "bookmarkId"
        const val CUSTOM_ACTION_BOOK_ID = "bookId"
        const val CUSTOM_ACTION_CHAPTER_ID = "chapterId"
        const val CUSTOM_ACTION_CHAPTER_POSITION = "chapterPosition"
        const val CUSTOM_ACTION_CHAPTER_DURATION = "chapterDuration"
        const val CUSTOM_ACTION_CHAPTER_TITLE = "chapterTitle"

        val commands = values().map { it.toCommand() }

        fun resolve(commandName: String): ExtraSessionCommands? = values().firstOrNull {
            it.commandName == commandName
        }

        fun isEnabled(args: Bundle) = args.getBoolean(CUSTOM_ACTION_ARG_ENABLED, false)

        fun getBookmarkId(args: Bundle) = args.getLong(CUSTOM_ACTION_BOOKMARK_ID, 0)

        fun getBookId(args: Bundle) = args.getLong(CUSTOM_ACTION_BOOK_ID, 0)

        fun getChapterId(args: Bundle) = args.getLong(CUSTOM_ACTION_CHAPTER_ID, 0)

        fun getChapterPosition(args: Bundle): ContentDuration =
            ContentDuration.ms(args.getLong(CUSTOM_ACTION_CHAPTER_POSITION, 0))

        fun getChapterDuration(args: Bundle): ContentDuration =
            ContentDuration.ms(args.getLong(CUSTOM_ACTION_CHAPTER_DURATION, 0))

        fun getChapterTitle(args: Bundle) =
            args.getString(CUSTOM_ACTION_CHAPTER_TITLE, "").ifEmpty { null }
    }
}
