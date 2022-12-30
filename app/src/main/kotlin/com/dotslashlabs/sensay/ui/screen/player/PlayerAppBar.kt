package com.dotslashlabs.sensay.ui.screen.player

import android.widget.Toast
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun PlayerAppBar(
    playerActions: PlayerActions,
    state: PlayerViewState,
    bottomSheetState: ModalBottomSheetState,
    scope: CoroutineScope = rememberCoroutineScope(),
    onBackPress: () -> Unit,
) {

    val context = LocalContext.current

    TopAppBar(
        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
        title = {},
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "",
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (!state.isEqPanelEnabled) return@IconButton

                    playerActions.toggleEqPanel(!state.isEqPanelVisible)
                },
                modifier = Modifier.alpha(
                    if (!state.isEqPanelEnabled || state.isEqPanelVisible) ContentAlpha.medium else ContentAlpha.high,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "",
                )
            }
            IconButton(
                onClick = {
                    if (!state.isBookmarkEnabled) return@IconButton

                    scope.launch {
                        try {
                            playerActions.createBookmark()
                            Toast.makeText(context, "Saved bookmark", Toast.LENGTH_SHORT).show()
                        } catch (ex: Exception) {
                            Toast.makeText(
                                context,
                                "Failed to save bookmark: ${ex.message}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
                modifier = Modifier.alpha(
                    if (!state.isBookmarkEnabled) ContentAlpha.disabled else ContentAlpha.high,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "",
                )
            }

            IconButton(onClick = {
                scope.launch { bottomSheetState.show() }
            }) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "",
                )
            }
        },
    )
}
