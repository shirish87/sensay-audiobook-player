package com.dotslashlabs.sensay.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.NotStarted
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dotslashlabs.sensay.ui.screen.common.ConfirmDialog
import data.BookCategory
import data.entity.BookProgressWithBookAndChapters

@Composable
fun BookContextMenu(
    @Suppress("UNUSED_PARAMETER") bookProgressWithChapters: BookProgressWithBookAndChapters,
    modifier: Modifier = Modifier,
) {

    if (bookProgressWithChapters.isEmpty) return

    val bookCategory = bookProgressWithChapters.bookProgress.bookCategory

    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val data: MutableState<BookProgressWithBookAndChapters?> = remember { mutableStateOf(null) }

    ConfirmDialog(
        data,
        title = { _ -> "Delete Book" },
        message = { d -> "Are you sure you want to delete ${d?.book?.title?.let { "'$it'" } ?: "this"} book?" },
        confirmLabel = "Delete",
        cancelLabel = "Cancel",
    ) {
        val book = it?.book ?: return@ConfirmDialog

        // TODO: actually hide selected book
        Toast.makeText(context, "Deleted book '${book.title}'", Toast.LENGTH_SHORT).show()
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

            when (bookCategory) {
                BookCategory.CURRENT -> {
                    DropdownMenuItem(
                        text = { Text("Mark as Not Started") },
                        onClick = { /* Handle settings! */ },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.NotStarted,
                                contentDescription = null
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Mark as Completed") },
                        onClick = { /* Handle settings! */ },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Done,
                                contentDescription = null
                            )
                        },
                    )
                }
                else -> {
                    DropdownMenuItem(
                        text = { Text("Mark as Current") },
                        onClick = { /* Handle settings! */ },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Subscriptions,
                                contentDescription = null
                            )
                        },
                    )
                }
            }

            Divider()

            DropdownMenuItem(
                text = { Text("Delete Book") },
                onClick = {
                    data.value = bookProgressWithChapters
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}