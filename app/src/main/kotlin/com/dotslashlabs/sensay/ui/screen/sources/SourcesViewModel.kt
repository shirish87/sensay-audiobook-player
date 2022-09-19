package com.dotslashlabs.sensay.ui.screen.sources

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.common.MediaSessionQueue
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import data.SensayStore
import data.entity.Source
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SourcesViewState(
    val sources: Async<List<Source>> = Uninitialized,
) : MavericksState

class SourcesViewModel @AssistedInject constructor(
    @Assisted private val state: SourcesViewState,
    @ApplicationContext private val context: Context,
    private val store: SensayStore,
    private val configStore: ConfigStore,
    private val mediaSessionQueue: MediaSessionQueue,
) : MavericksViewModel<SourcesViewState>(state) {

    init {
        store.sources()
            .map { it.sortedBy { r -> -r.createdAt.toEpochMilli() } }
            .execute(retainValue = SourcesViewState::sources) {
                copy(sources = it)
            }
    }

    fun addAudiobookFolders(uris: Set<Uri>) = viewModelScope.launch(Dispatchers.IO) {
        val sources = uris.mapNotNull { uri ->
            val f = DocumentFile.fromTreeUri(context, uri) ?: return@mapNotNull null
            val displayName = f.name ?: uri.toString().split("/").last()

            if (!f.isDirectory || !f.canRead()) {
                Source(
                    uri = uri,
                    displayName = displayName,
                    isActive = false,
                    inactiveReason = "No read access to directory",
                )
            } else {
                Source(uri = uri, displayName = displayName)
            }
        }

        if (store.addSources(sources) > 0) {
            configStore.setAudiobookFoldersUpdateTime(Instant.now())
        }
    }

    suspend fun deleteSource(sourceId: Long) {
        val bookIds = store.deleteSource(sourceId)
        if (bookIds.isNotEmpty()) {
            mediaSessionQueue.clearBooks(bookIds)
            configStore.setAudiobookFoldersUpdateTime(Instant.now())
        }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<SourcesViewModel, SourcesViewState> {
        override fun create(state: SourcesViewState): SourcesViewModel
    }

    companion object : MavericksViewModelFactory<SourcesViewModel, SourcesViewState>
    by hiltMavericksViewModelFactory()
}
