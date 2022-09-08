package com.dotslashlabs.sensay.ui.screen.player

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dotslashlabs.sensay.ui.screen.common.ConfirmDialog
import data.entity.Bookmark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PlayerBottomSheet(
    playerActions: PlayerActions,
    state: PlayerViewState,
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    ),
    content: @Composable (state: ModalBottomSheetState) -> Unit,
) {

    val bookmarks = state.bookmarks() ?: emptyList()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val data: MutableState<Bookmark?> = remember { mutableStateOf(null) }

    ConfirmDialog(
        data,
        title = { _ -> "Delete Bookmark" },
        message = { d -> "Are you sure you want to delete ${d?.title?.let { "'$it'" } ?: "this"} bookmark?" },
        confirmLabel = "Delete",
        cancelLabel = "Cancel",
    ) {
        val bookmark = it ?: return@ConfirmDialog

        scope.launch {
            playerActions.deleteBookmark(bookmark)
            Toast.makeText(context, "Deleted bookmark", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        content = { content(bottomSheetState) },
        sheetShape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp),
        sheetContent = {

            LazyColumn(modifier = Modifier.systemBarsPadding().padding(bottom = 20.dp)) {
                stickyHeader {
                    Text(
                        "Bookmarks",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    )
                }

                if (bookmarks.isEmpty()) {
                    return@LazyColumn items(1) {
                        ListItem(headlineText = { Text("Nothing found.") })
                    }
                }

                items(bookmarks.size) { index ->
                    val (bookmark, chapter) = bookmarks[index]

                    ListItem(
                        modifier = Modifier.clickable {
                            scope.launch {
                                playerActions.seekToPosition(
                                    PlayerViewState.getMediaId(bookmark.bookId, bookmark.chapterId),
                                    bookmark.chapterPosition.ms,
                                    bookmark.chapterDuration.ms,
                                )

                                bottomSheetState.hide()
                            }
                        },
                        headlineText = { Text(bookmark.title ?: chapter.title) },
                        supportingText = {
                            Text(
                                "at ${
                                    bookmark.chapterPosition.format()
                                } of ${
                                    bookmark.chapterDuration.format()
                                }"
                            )
                        },
                        trailingContent = {
                            androidx.compose.material3.IconButton(onClick = {
                                scope.launch {
                                    data.value = bookmark
                                }
                            }) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "",
                                )
                            }
                        },
                    )
                }
            }
        },
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    )
}
