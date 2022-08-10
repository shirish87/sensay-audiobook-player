package com.dotslashlabs.sensay.ui.screen.home

import android.content.Context
import android.net.Uri
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
import data.util.Time
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import scanner.CoverScanner
import scanner.MediaScanner

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

data class HomeViewState(
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val audiobookFolders: Async<Set<Uri>> = Uninitialized,
    val isScanningFolders: Boolean = false,
) : MavericksState

class HomeViewModel @AssistedInject constructor(
    @Assisted private val state: HomeViewState,
    private val store: SensayStore,
    private val configStore: ConfigStore,
    private val mediaScanner: MediaScanner,
    private val coverScanner: CoverScanner,
) : MavericksViewModel<HomeViewState>(state) {

    init {
        configStore.getHomeLayout().execute {
            copy(homeLayout = it() ?: this.homeLayout)
        }

        configStore.getAudiobookFolders().execute(retainValue = HomeViewState::audiobookFolders) {
            copy(audiobookFolders = it)
        }
    }

    fun booksCount() = store.booksCount()

    fun setHomeLayout(layout: HomeLayout) = viewModelScope.launch {
        configStore.setHomeLayout(layout)
    }

    suspend fun createBooksWithChapters(booksWithChapters: List<BookWithChapters>) =
        store.createBooksWithChapters(booksWithChapters)

    fun addAudiobookFolders(uris: Set<Uri>) = viewModelScope.launch {
        configStore.addAudiobookFolders(uris)
    }

    private fun setScanningFolders(isScanningFolders: Boolean) {
        setState { copy(isScanningFolders = isScanningFolders) }
    }

    fun scanFolders(context: Context) {
        withState {
            val audiobookFolders = it.audiobookFolders.invoke() ?: return@withState

            viewModelScope.launch {
                try {
                    setScanningFolders(true)
                    scanFolders(context, audiobookFolders)
                } finally {
                    setScanningFolders(false)
                }
            }
        }
    }

    private suspend fun scanFolders(
        context: Context,
        audiobookFolders: Set<Uri>,
        batchSize: Int = 2,
    ): Int {
        if (audiobookFolders.isEmpty()) {
            return 0
        }

        val folders: List<DocumentFile> = audiobookFolders.mapNotNull { uri ->
            DocumentFile.fromTreeUri(context, uri)
        }

        val newBooks = mutableListOf<BookWithChapters>()
        var bookCount = 0

        mediaScanner.scan(
            folders,
            { fileUri -> (store.booksByUri(fileUri).firstOrNull() == null) },
        ) { folderUri, f, metadata ->
            val bookHash = metadata.hash
            val coverFile = coverScanner.scanCover(folderUri, f.uri, bookHash)

            newBooks.add(
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
                            hash = chapter.hash,
                            uri = f.uri,
                            trackId = chapter.id,
                            title = chapter.title,
                            start = Time(chapter.start),
                            end = Time(chapter.end),
                            duration = Time(chapter.duration),
                        )
                    }
                )
            )

            if (newBooks.size >= batchSize) {
                bookCount += store.createBooksWithChapters(newBooks).size
                newBooks.clear()
            }
        }

        bookCount += store.createBooksWithChapters(newBooks).size
        return bookCount
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<HomeViewModel, HomeViewState> {
        override fun create(state: HomeViewState): HomeViewModel
    }

    companion object : MavericksViewModelFactory<HomeViewModel, HomeViewState>
    by hiltMavericksViewModelFactory()
}
