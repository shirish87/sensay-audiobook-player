package com.dotslashlabs.sensay.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dotslashlabs.sensay.ui.screen.common.ConfirmDialog
import com.dotslashlabs.sensay.ui.screen.restore.OnNavToRestore
import data.BookCategory
import data.entity.BookProgressWithBookAndChapters

typealias OnSetBookCategory = (BookProgressWithBookAndChapters, BookCategory) -> Unit

typealias OnBookVisibilityChange = (BookProgressWithBookAndChapters, Boolean) -> Unit

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
    modifier: Modifier = Modifier,
) {

    if (bookProgressWithChapters.isEmpty || !config.isEnabled) return

    val bookCategory = bookProgressWithChapters.bookProgress.bookCategory

    val categoryMenuItems = listOf(
        BookCategory.NOT_STARTED to Icons.Outlined.NotStarted,
        BookCategory.CURRENT to Icons.Outlined.Subscriptions,
        BookCategory.FINISHED to Icons.Outlined.Done,
    ).filterNot { it.first == bookCategory }

    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val hideData: MutableState<BookProgressWithBookAndChapters?> =
        remember { mutableStateOf(null) }

    ConfirmDialog(
        hideData,
        title = { "Hide Book" },
        message = { d ->
            "Are you sure you want to hide ${d?.book?.title ?: "this"} book?"
        },
        confirmLabel = "Hide",
        cancelLabel = "Cancel",
    ) { arg ->

        if (arg != null) {
            val book = arg.book

            config.onBookVisibilityChange(arg, false)

            Toast.makeText(
                context,
                "Book '${book.title}' has been hidden from view.",
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopEnd),
    ) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
        }
        DropdownMenu(
            modifier = Modifier,
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            categoryMenuItems.forEach { (category, imageVector) ->
                DropdownMenuItem(
                    text = { Text("Mark as ${category.label}") },
                    onClick = {
                        changeCategoryData.value = bookProgressWithChapters to category
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (config.isRestoreBookEnabled &&
                bookProgressWithChapters.bookProgress.bookCategory == BookCategory.NOT_STARTED
            ) {

                DropdownMenuItem(
                    text = { Text("Restore Progress") },
                    onClick = {
                        expanded = false
                        config.onNavToRestore(bookProgressWithChapters.bookProgress.bookId)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.RestorePage,
                            contentDescription = null,
                        )
                    },
                )
            }

            if (config.isVisibilityChangeEnabled) {
                Divider()

                DropdownMenuItem(
                    text = { Text("Hide Book") },
                    onClick = {
                        hideData.value = bookProgressWithChapters
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}
