package com.dotslashlabs.sensay.ui.screen.sources

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ui.SensayAppState
import com.dotslashlabs.sensay.ui.SensayAppViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.ConfirmDialog
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import data.entity.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SourcesScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry
    ) = SourcesContent(
        destination,
        navHostController,
        backStackEntry,
        onBackPress = { navHostController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SourcesContent(
    @Suppress("UNUSED_PARAMETER") destination: Destination,
    @Suppress("UNUSED_PARAMETER") navHostController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
    @Suppress("UNUSED_PARAMETER") onBackPress: () -> Unit,
) {

    val appViewModel: SensayAppViewModel = mavericksActivityViewModel()
    val isScanningFolders by appViewModel.collectAsState(SensayAppState::isScanningFolders)

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

        viewModel.addAudiobookFolders(setOf(uri)).invokeOnCompletion {
            appViewModel.scanFolders(context, force = true)
        }
    }

    val data: MutableState<Source?> = remember { mutableStateOf(null) }

    ConfirmDialog(
        data,
        title = { "Delete Source" },
        message = { d ->
            "Are you sure you want to delete ${
            d?.displayName?.let { "'$it'" } ?: "this"
            } source?"
        },
        confirmLabel = "Delete",
        cancelLabel = "Cancel",
    ) {
        val source = it ?: return@ConfirmDialog

        viewModel.viewModelScope.launch(Dispatchers.IO) {
            viewModel.deleteSource(source.sourceId)

            withContext(Dispatchers.Main) {
                appViewModel.scanFolders(context)
                Toast.makeText(context, "Deleted source", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SensayFrame {
        Scaffold(
            modifier = Modifier.align(Alignment.Center),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    title = { Text("Sources") },
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
                            enabled = !isScanningFolders,
                            onClick = { appViewModel.scanFolders(context, true) },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
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
        ) { paddingValues ->
            val listState: LazyListState = rememberLazyListState()

            LazyColumn(state = listState, contentPadding = paddingValues) {
                val audiobookFolders = ((state.sources as? Success?)?.invoke() ?: emptyList())

                items(audiobookFolders, key = { it.sourceId }) { item ->
                    ListItem(
                        headlineText = { Text(text = item.displayName) },
                        supportingText = { Text(text = item.uri.path ?: "") },
                        leadingContent = {
                            if (!item.isActive) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = "",
                                )
                            } else {
                                IconButton(
                                    enabled = !isScanningFolders,
                                    onClick = {
                                        appViewModel.scanFolders(
                                            context,
                                            true,
                                            item.sourceId,
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "",
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { data.value = item }) {
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
