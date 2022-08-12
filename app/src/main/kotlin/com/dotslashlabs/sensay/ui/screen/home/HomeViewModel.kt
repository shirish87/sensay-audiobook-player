package com.dotslashlabs.sensay.ui.screen.home

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.Book
import data.entity.BookWithChapters
import data.entity.Chapter
import data.entity.Source
import data.util.Time
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import scanner.CoverScanner
import scanner.MediaScanner
import java.time.Instant

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

data class HomeViewState(
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val isScanningFolders: Boolean = false,
    val audiobookFoldersUpdateTime: Async<Long?> = Uninitialized,
    val lastScanTime: Long = 0,
) : MavericksState {

    val shouldScan = ((audiobookFoldersUpdateTime.invoke() ?: 0L) > lastScanTime)
}

class HomeViewModel @AssistedInject constructor(
    @Assisted private val state: HomeViewState,
    private val store: SensayStore,
    private val configStore: ConfigStore,
    private val mediaScanner: MediaScanner,
    private val coverScanner: CoverScanner,
) : MavericksViewModel<HomeViewState>(state) {

    private var scannerJob : Job? = null

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it() ?: this.homeLayout)
        }

        configStore.getAudiobookFoldersUpdateTime()
            .map { it.toEpochMilli() }
            .execute(retainValue = HomeViewState::audiobookFoldersUpdateTime) {
                copy(audiobookFoldersUpdateTime = it)
            }
    }

    fun setHomeLayout(layout: HomeLayout) = viewModelScope.launch {
        configStore.setHomeLayout(layout)
    }

    private fun setScanningFolders(isScanningFolders: Boolean) {
        setState { copy(isScanningFolders = isScanningFolders) }
    }

    fun scanFolders(context: Context) = withState { state ->
        if (!state.shouldScan) return@withState

        cancelScanFolders()

        scannerJob = viewModelScope.launch {
            try {
                setScanningFolders(true)

                val activeSources = (store.sources(isActive = true).firstOrNull() ?: emptyList())
                    .sortedBy { -it.createdAt.value }

                if (activeSources.isEmpty()) return@launch

                scanFolders(context, activeSources)
                setState { copy(lastScanTime = Instant.now().toEpochMilli()) }
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
        context: Context,
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
                            duration = Time(metadata.duration),
                            hash = bookHash,
                            coverUri = coverFile?.uri,
                        ),
                        chapters = metadata.chapters.map { chapter ->
                            Chapter(
                                uri = f.uri,
                                hash = chapter.hash,
                                trackId = chapter.id,
                                title = chapter.title,
                                start = Time(chapter.start),
                                end = Time(chapter.end),
                                duration = Time(chapter.duration),
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

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState>
    by hiltMavericksViewModelFactory()
}
