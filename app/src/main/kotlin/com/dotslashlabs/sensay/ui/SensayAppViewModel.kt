package com.dotslashlabs.sensay.ui

import android.content.Context
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.documentfile.provider.DocumentFile
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.util.DevicePosture
import com.dotslashlabs.sensay.util.WindowSizeClass
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import data.SensayStore
import data.entity.*
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import scanner.CoverScanner
import scanner.MediaScanner
import java.time.Instant

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

data class SensayAppState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val isScanningFolders: Boolean = false,
    val audiobookFoldersUpdateTime: Async<Long?> = Uninitialized,
    val lastScanTime: Instant = Instant.EPOCH,
    val windowSize: WindowSizeClass = WindowSizeClass.default(),
    val devicePosture: DevicePosture = DevicePosture.default(),
) : MavericksState {

    val shouldScan = ((audiobookFoldersUpdateTime() ?: 0L) > lastScanTime.toEpochMilli())

    val useLandscapeLayout = (windowSize.heightSizeClass == WindowHeightSizeClass.Compact)
}

class SensayAppViewModel @AssistedInject constructor(
    @Assisted private val state: SensayAppState,
    @ApplicationContext private val context: Context,
    private val store: SensayStore,
    private val configStore: ConfigStore,
    private val mediaScanner: MediaScanner,
    private val coverScanner: CoverScanner,
) : MavericksViewModel<SensayAppState>(state) {

    private var scannerJob : Job? = null

    init {
        store.booksProgressWithBookAndChapters().execute {
            copy(books = it)
        }

        configStore.getHomeLayout().execute {
            copy(homeLayout = it() ?: this.homeLayout)
        }

        configStore.getAudiobookFoldersUpdateTime()
            .map { it.toEpochMilli() }
            .execute(retainValue = SensayAppState::audiobookFoldersUpdateTime) {
                copy(audiobookFoldersUpdateTime = it)
            }
    }

    fun configure(windowSize: WindowSizeClass, devicePosture: DevicePosture) = setState {
        copy(windowSize = windowSize, devicePosture = devicePosture)
    }

    fun setHomeLayout(layout: HomeLayout) = viewModelScope.launch {
        configStore.setHomeLayout(layout)
    }

    private fun setScanningFolders(isScanningFolders: Boolean) {
        setState { copy(isScanningFolders = isScanningFolders) }
    }

    fun scanFolders(force: Boolean = false) = withState { state ->
        if (!force && !state.shouldScan) return@withState

        cancelScanFolders()

        scannerJob = viewModelScope.launch {
            try {
                setScanningFolders(true)

                withContext(Dispatchers.IO) {
                    val activeSources = (store.sources(isActive = true).firstOrNull() ?: emptyList())
                        .sortedBy { -it.createdAt.toEpochMilli() }

                    if (activeSources.isEmpty()) return@withContext 0

                    return@withContext scanFolders(activeSources)
                }

                setState { copy(lastScanTime = Instant.now()) }
            } finally {
                setScanningFolders(false)
            }
        }
    }

    fun cancelScanFolders() {
        if (scannerJob?.isActive == true) {
            scannerJob?.cancel()
        }
    }

    private suspend fun scanFolders(
        activeSources: Collection<Source>,
        batchSize: Int = 4,
    ): Int {

        if (activeSources.isEmpty()) {
            return 0
        }

        val sourceDocumentFiles = activeSources.mapNotNull { source ->
            val df = DocumentFile.fromTreeUri(context, source.uri)
            if (df == null || !df.isDirectory || !df.canRead()) return@mapNotNull null

            source to df
        }

        return sourceDocumentFiles.fold(0) { totalBookCount, sourceDocumentFile ->
            val (source, df) = sourceDocumentFile
            val sourceBooks = mutableListOf<BookWithChapters>()
            var sourceBookCount = 0

            mediaScanner.scan(
                listOf(df),
                { file -> (store.bookByUri(file.uri).firstOrNull() == null) },
            ) { f, metadata ->
                val bookHash = metadata.hash
                val coverFile = coverScanner.scanCover(f.parentFile?.uri, f.uri, bookHash)

                sourceBooks.add(
                    BookWithChapters(
                        book = Book(
                            uri = f.uri,
                            author = metadata.author,
                            series = metadata.album,
                            title = metadata.title,
                            duration = ContentDuration(metadata.duration),
                            hash = bookHash,
                            coverUri = coverFile?.uri,
                        ),
                        chapters = metadata.chapters.map { chapter ->
                            Chapter(
                                uri = f.uri,
                                hash = chapter.hash,
                                trackId = chapter.id,
                                title = chapter.title,
                                start = ContentDuration(chapter.start),
                                end = ContentDuration(chapter.end),
                                duration = ContentDuration(chapter.duration),
                            )
                        }
                    )
                )

                if (sourceBooks.size >= batchSize) {
                    sourceBookCount += store.createBooksWithChapters(
                        source.sourceId,
                        sourceBooks
                    ).size
                    sourceBooks.clear()
                }
            }

            if (sourceBooks.isNotEmpty()) {
                sourceBookCount += store.createBooksWithChapters(source.sourceId, sourceBooks).size
            }

            totalBookCount + sourceBookCount
        }
    }

    suspend fun getLastPlayedBookId() = configStore.getLastPlayedBookId().first()

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<SensayAppViewModel, SensayAppState> {
        override fun create(state: SensayAppState): SensayAppViewModel
    }

    companion object : MavericksViewModelFactory<SensayAppViewModel, SensayAppState>
    by hiltMavericksViewModelFactory()
}
