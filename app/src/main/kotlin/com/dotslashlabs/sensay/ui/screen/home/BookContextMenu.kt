package com.dotslashlabs.sensay.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dotslashlabs.sensay.ui.common.ConfirmDialog
import compose.icons.MaterialIcons
import compose.icons.materialicons.Done
import compose.icons.materialicons.NotStarted
import compose.icons.materialicons.RestorePage
import compose.icons.materialicons.Subscriptions
import compose.icons.materialicons.Visibility
import compose.icons.materialicons.VisibilityOff
import data.BookCategory
import data.entity.Book
import data.entity.BookProgressWithBookAndChapters

typealias OnSetBookCategory = (BookProgressWithBookAndChapters, BookCategory) -> Unit

typealias OnBookVisibilityChange = (BookProgressWithBookAndChapters, Boolean) -> Unit

typealias OnNavToBook = (bookId: Long) -> Unit

typealias OnBookLookup = (book: Book) -> Unit

typealias OnPlay = ((bookProgressWithChapters: BookProgressWithBookAndChapters) -> Unit)

typealias OnNavToRestore = (bookId: Long) -> Unit

data class BookContextMenuConfig(
    val isEnabled: Boolean = true,
    val isRestoreBookEnabled: Boolean = false,
    val isVisibilityChangeEnabled: Boolean = false,
    val onSetBookCategory: OnSetBookCategory,
    val onBookVisibilityChange: OnBookVisibilityChange,
    val onNavToRestore: OnNavToRestore,
)

@Composable
fun BookContextMenu(
    bookProgressWithChapters: BookProgressWithBookAndChapters,
    config: BookContextMenuConfig,
    isMenuExpanded: Boolean,
    setMenuExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onPlay: OnPlay? = null,
    onBookLookup: OnBookLookup? = null,
) {

    if (bookProgressWithChapters.isEmpty || !config.isEnabled) return

    val bookCategory = bookProgressWithChapters.bookProgress.bookCategory

    val categoryMenuItems = listOf(
        BookCategory.NOT_STARTED to MaterialIcons.NotStarted,
        BookCategory.CURRENT to MaterialIcons.Subscriptions,
        BookCategory.FINISHED to MaterialIcons.Done,
    ).filterNot { it.first == bookCategory }

    val context = LocalContext.current

    val visibilityData: MutableState<Pair<BookProgressWithBookAndChapters, Boolean>?> =
        remember { mutableStateOf(null) }

    ConfirmDialog(
        visibilityData,
        title = { d -> "${if (d?.second == true) "Hide" else "Show"} Book" },
        message = { d ->
            val action = if (d?.second == true) "hide" else "show"
            "Are you sure you want to $action ${d?.first?.book?.title ?: "this"} book?"
        },
        confirmLabel = "Confirm",
        cancelLabel = "Cancel",
    ) { arg ->

        if (arg != null) {
            val book = arg.first.book

            config.onBookVisibilityChange(arg.first, !arg.second)

            Toast.makeText(
                context,
                "Book '${book.title}' ${
                if (!arg.second)
                    "is now visible."
                else "has been hidden from view."
                }",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val changeCategoryData: MutableState<Pair<BookProgressWithBookAndChapters, BookCategory>?> =
        remember { mutableStateOf(null) }

    ConfirmDialog(
        changeCategoryData,
        title = { arg -> "Mark as ${arg?.second?.label ?: ""}" },
        message = { arg ->
            "Are you sure you want to mark '${
            arg?.first?.book?.title ?: ""
            }' book as '${
            arg?.second?.label ?: ""
            }'?${
            if (arg?.second != BookCategory.CURRENT)
                " You will lose any associated progress information."
            else ""
            }"
        },
        confirmLabel = "Confirm",
        cancelLabel = "Cancel",
    ) { arg ->

        if (arg != null) {
            config.onSetBookCategory(
                bookProgressWithChapters,
                arg.second,
            )

            Toast.makeText(
                context,
                "Book '${arg.first.book.title}' set to ${arg.second.label}",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    onPlay?.let {
        Box(
            modifier = modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart),
        ) {
            if (bookCategory == BookCategory.FINISHED) {
                IconButton(onClick = {
                    Toast.makeText(
                        context,
                        "Finished on ${bookProgressWithChapters.bookProgress.lastUpdatedAt}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Localized description",
                        tint = Color.Green,
                    )
                }
            } else {
                IconButton(onClick = { it(bookProgressWithChapters) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Localized description")
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopEnd),
    ) {
        IconButton(onClick = { setMenuExpanded(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
        }
        DropdownMenu(
            modifier = Modifier,
            expanded = isMenuExpanded,
            onDismissRequest = { setMenuExpanded(false) }
        ) {

            DropdownMenuItem(
                text = { Text("Book Lookup") },
                onClick = {
                    setMenuExpanded(false)
                    onBookLookup?.invoke(bookProgressWithChapters.book)
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                    )
                },
            )

            categoryMenuItems.forEach { (category, imageVector) ->
                DropdownMenuItem(
                    text = { Text("Mark as ${category.label}") },
                    onClick = {
                        changeCategoryData.value = bookProgressWithChapters to category
                        setMenuExpanded(false)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (config.isRestoreBookEnabled && bookCategory == BookCategory.NOT_STARTED) {
                DropdownMenuItem(
                    text = { Text("Restore Progress") },
                    onClick = {
                        setMenuExpanded(false)
                        config.onNavToRestore(bookProgressWithChapters.bookProgress.bookId)
                    },
                    leadingIcon = {
                        Icon(
                            MaterialIcons.RestorePage,
                            contentDescription = null,
                        )
                    },
                )
            }

            val isBookVisible = bookProgressWithChapters.bookProgress.isVisible

            if (config.isVisibilityChangeEnabled || !isBookVisible) {
                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("${if (isBookVisible) "Hide" else "Show"} Book") },
                    onClick = {
                        visibilityData.value = bookProgressWithChapters to isBookVisible
                        setMenuExpanded(false)
                    },
                    leadingIcon = {
                        Icon(
                            if (isBookVisible)
                                MaterialIcons.VisibilityOff
                            else MaterialIcons.Visibility,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
