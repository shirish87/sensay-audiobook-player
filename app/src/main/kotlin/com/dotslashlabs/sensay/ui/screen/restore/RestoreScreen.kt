package com.dotslashlabs.sensay.ui.screen.restore

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.dotslashlabs.sensay.ui.screen.Destination
import com.dotslashlabs.sensay.ui.screen.SensayScreen
import com.dotslashlabs.sensay.ui.screen.common.ConfirmDialog
import com.dotslashlabs.sensay.ui.screen.common.SensayFrame
import com.dotslashlabs.sensay.ui.screen.home.ListBookView
import data.entity.BookProgressWithBookAndChapters
import data.entity.Progress
import logcat.logcat

typealias OnNavToRestore = (bookId: Long) -> Unit

object RestoreScreen : SensayScreen {
    @Composable
    override fun Content(
        destination: Destination,
        navHostController: NavHostController,
        backStackEntry: NavBackStackEntry,
    ) = RestoreContent(
        destination,
        navHostController,
        backStackEntry,
        onBackPress = { navHostController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RestoreContent(
    @Suppress("UNUSED_PARAMETER") destination: Destination,
    @Suppress("UNUSED_PARAMETER") navHostController: NavHostController,
    @Suppress("UNUSED_PARAMETER") backStackEntry: NavBackStackEntry,
    @Suppress("UNUSED_PARAMETER") onBackPress: () -> Unit,
) {

    val argsBundle = backStackEntry.arguments ?: return

    val viewModel: RestoreViewModel =
        mavericksViewModel(argsFactory = { RestoreViewArgs(argsBundle) })

    val state by viewModel.collectAsState()
    logcat("RestoreContent") { "state=$state" }
    val bookProgressWithChapters = state.bookProgressWithBookAndChapters() ?: return

    val conditions: List<Pair<String, (Progress) -> Boolean>> = listOf(
        "Matching Duration and Chapter Count" to { p ->
            p.bookDuration.ms == bookProgressWithChapters.book.duration.ms &&
                p.totalChapters == bookProgressWithChapters.chapters.size
        },
        "Matching Chapter Count" to { p ->
            p.totalChapters == bookProgressWithChapters.chapters.size
        },
        "Matching Duration" to { p ->
            p.bookDuration.ms == bookProgressWithChapters.book.duration.ms
        },
    )

    val context = LocalContext.current
    val deleteData: MutableState<Progress?> = remember { mutableStateOf(null) }

    ConfirmDialog(
        deleteData,
        title = { "Delete Progress" },
        message = { d ->
            "Are you sure you want to delete ${
            d?.bookTitle?.let { "'$it'" } ?: "this"
            } progress?"
        },
        confirmLabel = "Delete",
        cancelLabel = "Cancel",
    ) {
        val progress = it ?: return@ConfirmDialog

        viewModel.deleteProgress(progress)

        Toast.makeText(
            context,
            "Deleted book progress for '${progress.bookTitle}'",
            Toast.LENGTH_SHORT,
        ).show()
    }

    val onDelete: (Progress) -> Unit = { deleteData.value = it }

    val restoreData: MutableState<Progress?> = remember { mutableStateOf(null) }

    ConfirmDialog(
        restoreData,
        title = { "Restore Progress" },
        message = {
            "Are you sure you want to restore progress for book '${
            bookProgressWithChapters.book.title
            }'?"
        },
        confirmLabel = "Restore",
        cancelLabel = "Cancel",
    ) {
        val progress = it ?: return@ConfirmDialog

        viewModel.restoreBookProgress(bookProgressWithChapters, progress)

        Toast.makeText(
            context,
            "Restored book progress for '${progress.bookTitle}'",
            Toast.LENGTH_SHORT,
        ).show()

        onBackPress()
    }

    val onRestore: (Progress) -> Unit = { restoreData.value = it }

    SensayFrame {
        Scaffold(
            modifier = Modifier.align(Alignment.Center),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                    title = { Text("Restore Progress") },
                    navigationIcon = {
                        IconButton(onClick = onBackPress) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "",
                            )
                        }
                    },
                    actions = {},
                )
            },
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
            ) {
                val list: List<Progress> = state.progressRestorable() ?: emptyList()

                ElevatedCard(
                    onClick = {},
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Box {
                        ListBookView(bookProgressWithChapters)
                    }
                }

                val others = conditions.fold(list) { acc, condition ->
                    val (matching, remaining) = acc.partition(condition.second)

                    if (matching.isNotEmpty()) {
                        ListBookProgressUnassociated(
                            list = matching,
                            title = condition.first,
                            onDelete = onDelete,
                            onRestore = onRestore,
                        )
                    }

                    remaining
                }

                if (others.isNotEmpty()) {
                    ListBookProgressUnassociated(
                        list = others,
                        title = "Incompatible",
                        isRestoreEnabled = false,
                        onDelete = onDelete,
                        onRestore = onRestore,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListBookProgressUnassociated(
    list: List<Progress>,
    title: String,
    modifier: Modifier = Modifier,
    isRestoreEnabled: Boolean = true,
    onDelete: (Progress) -> Unit,
    onRestore: (Progress) -> Unit,
) {
    val listState: LazyListState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {

        stickyHeader {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
        }

        items(list, key = { it.progressId }) { item ->
            val bookProgressWithChapters = BookProgressWithBookAndChapters.empty().run {
                copy(
                    bookProgress = item.toBookProgress(
                        bookProgressId = 0L,
                        bookId = 0L,
                        chapterId = 0L,
                    ),
                    book = book.copy(
                        title = item.bookTitle,
                        author = item.bookAuthor,
                        duration = item.bookDuration,
                    ),
                    chapter = chapter.copy(
                        title = item.chapterTitle,
                    ),
                )
            }

            Box {

                Column(modifier = Modifier.fillMaxWidth()) {
                    ListBookView(
                        bookProgressWithChapters,
                        height = 90.dp,
                        useCoverImage = false,
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 10.dp),
                        text = bookProgressWithChapters.chapter.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {

                        TextButton(
                            onClick = { onDelete(item) },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier,
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(text = "Delete")
                        }

                        Button(
                            enabled = isRestoreEnabled,
                            onClick = { onRestore(item) },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier,
                        ) {
                            Text(text = "Resume at ${item.bookProgress.formatFull()}")
                        }
                    }
                }
            }

            Divider()
        }
    }
}
