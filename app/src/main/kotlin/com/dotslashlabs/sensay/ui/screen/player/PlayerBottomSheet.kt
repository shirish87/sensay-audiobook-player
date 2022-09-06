package com.dotslashlabs.sensay.ui.screen.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        content = { content(bottomSheetState) },
        sheetShape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp),
        sheetContent = {

            LazyColumn(modifier = Modifier.systemBarsPadding()) {
                stickyHeader {
                    Text(
                        "Bookmarks",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
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
                                    bookmark.chapterPosition.formatFull()
                                } of ${
                                    bookmark.chapterDuration.formatFull()
                                }"
                            )
                        },
                    )
                }
            }
        },
    )
}
