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
import data.SensayStore
import data.entity.Source
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant

data class SourcesViewState(
    val sources: Async<List<Source>> = Uninitialized,
) : MavericksState

class SourcesViewModel @AssistedInject constructor(
    @Assisted private val state: SourcesViewState,
    private val store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<SourcesViewState>(state) {

    init {
        store.sources()
            .map { it.sortedBy { r -> -r.createdAt.value } }
            .execute(retainValue = SourcesViewState::sources) {
                copy(sources = it)
            }
    }

    fun addAudiobookFolders(context: Context, uris: Set<Uri>) = viewModelScope.launch {
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

    fun deleteSource(sourceId: Long) = viewModelScope.launch {
        if (store.deleteSource(sourceId)) {
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
