package com.dotslashlabs.sensay.ui.screen.sources

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ui.common.ConfirmDialog
import com.dotslashlabs.sensay.ui.common.ScannerViewModel
import com.dotslashlabs.sensay.ui.common.ScannerViewState
import com.dotslashlabs.sensay.ui.nav.AppScreen
import com.dotslashlabs.sensay.ui.nav.Destination
import compose.icons.MaterialIcons
import compose.icons.materialicons.Error
import data.entity.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


typealias OnNavToSources = () -> Unit

object SourcesScreen : AppScreen {

    @OptIn(ExperimentalCoroutinesApi::class)
    @UnstableApi
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) {

        val scannerViewModel: ScannerViewModel = mavericksActivityViewModel()
        val isScanningFolders by scannerViewModel.collectAsState(ScannerViewState::isScanningFolders)

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

            viewModel.addAudiobookFolders(setOf(uri))
                .run {
                    invokeOnCompletion {
                        val sourceId = getCompleted().firstOrNull()?.sourceId
                        scannerViewModel.scanFolders(context, force = true, sourceId = sourceId)
                    }
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
                    scannerViewModel.scanFolders(context)
                    Toast.makeText(context, "Deleted source", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Scaffold(
            topBar = {

            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Filled.AddCircle, "Add Folder") },
                    text = { Text("Add Folder") },
                    onClick = { launcher.launch(null) },
                )
            },
        ) { paddingValues ->

            val listState: LazyListState = rememberLazyListState()

            LazyColumn(state = listState, contentPadding = paddingValues) {
                val audiobookFolders = ((state.sources as? Success?)?.invoke() ?: emptyList())

                items(audiobookFolders, key = { it.sourceId }) { item ->
                    ListItem(
                        headlineContent = { Text(text = item.displayName) },
                        supportingContent = { Text(text = item.uri.path ?: "") },
                        leadingContent = {
                            if (!item.isActive) {
                                Icon(
                                    MaterialIcons.Error,
                                    contentDescription = "",
                                )
                            } else {
                                IconButton(
                                    enabled = !isScanningFolders,
                                    onClick = {
                                        scannerViewModel.scanFolders(
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
                    HorizontalDivider()
                }
            }
        }
    }
}
