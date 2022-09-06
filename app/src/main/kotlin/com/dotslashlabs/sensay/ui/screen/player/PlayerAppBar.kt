package com.dotslashlabs.sensay.ui.screen.player

import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

    SmallTopAppBar(
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
                onClick = if (!state.isBookmarkEnabled) ({}) else playerActions::createBookmark,
                modifier = Modifier.alpha(if (!state.isBookmarkEnabled) ContentAlpha.disabled else 1F),
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
