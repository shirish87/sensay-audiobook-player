package com.dotslashlabs.sensay.ui.screen.sources

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import data.SensayStore
import data.entity.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant


data class SourcesViewState(
    val sources: Async<List<Source>> = Uninitialized,
) : MavericksState

class SourcesViewModel @AssistedInject constructor(
    @Assisted private val state: SourcesViewState,
    @ApplicationContext private val context: Context,
    private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<SourcesViewState>(state) {

    init {
        store.sources()
            .map { it.sortedBy { r -> -r.createdAt.toEpochMilli() } }
            .execute(retainValue = SourcesViewState::sources) {
                copy(sources = it)
            }
    }

    fun addAudiobookFolders(uris: Set<Uri>) = viewModelScope.async(Dispatchers.IO) {
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

        val storeIds = store.addSources(sources)

        return@async if (storeIds.isNotEmpty()) {
            configStore.setAudiobookFoldersUpdateTime(Instant.now())
            sources.mapIndexed { idx, s -> s.copy(sourceId = storeIds[idx]) }
        } else emptyList()
    }

    suspend fun deleteSource(sourceId: Long) {
        val bookIds = store.deleteSource(sourceId)

        if (bookIds.isNotEmpty()) {
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
