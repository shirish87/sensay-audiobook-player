package com.dotslashlabs.sensay.ui.screen.sources

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ActivityBridge
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame

object SourcesScreen : SensayScreen {
    @Composable
    override fun content(
        destination: Destination,
        activityBridge: ActivityBridge,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry
    ) = SourcesContent(
        destination,
        activityBridge,
        navHostController,
        backStackEntry,
        onBackPress = { navHostController.popBackStack() })
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SourcesContent(
    @Suppress("UNUSED_PARAMETER") destination: Destination,
    @Suppress("UNUSED_PARAMETER") activityBridge: ActivityBridge,
    @Suppress("UNUSED_PARAMETER") navHostController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
    @Suppress("UNUSED_PARAMETER") onBackPress: () -> Unit,
) {

    val viewModel: SourcesViewModel = mavericksViewModel(backStackEntry)
    val state by viewModel.collectAsState()

    val context: Context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        viewModel.addAudiobookFolders(context, setOf(uri))
    }

    SensayFrame {
        Scaffold(
            modifier = Modifier.align(Alignment.Center),
            topBar = {
                SmallTopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Sources") },
                    navigationIcon = {
                        IconButton(onClick = onBackPress) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "",
                            )
                        }
                    },
                )
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.AddCircle, "Add Folder") },
                    text = { Text("Add Folder") },
                    onClick = { launcher.launch(null) }
                )
            },
        ) {
            val listState: LazyListState = rememberLazyListState()

            LazyColumn(state = listState, contentPadding = it) {
                val audiobookFolders = ((state.sources as? Success?)?.invoke() ?: emptyList())

                items(count = audiobookFolders.size) { index ->
                    val item = audiobookFolders[index]

                    ListItem(
                        headlineText = { Text(text = item.displayName) },
                        supportingText = { Text(text = item.uri.path ?: "") },
                        leadingContent = {
                            if (!item.isActive) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = "",
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteSource(item.sourceId) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "",
                                )
                            }
                        },
                    )
                    Divider()
                }
            }
        }
    }
}
