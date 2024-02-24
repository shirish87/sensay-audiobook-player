package com.dotslashlabs.sensay.ui.common

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.media3.common.util.UnstableApi
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.scanner.BookScannerWorker
import config.ConfigStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.entity.SourceId
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID


data class ScannerViewState(
    val isScanningFolders: Boolean = false,
    val audiobookFoldersUpdateTime: Async<Long?> = Uninitialized,
    val lastScanTime: Long = Instant.EPOCH.toEpochMilli(),
) : MavericksState {

    val shouldScan = ((audiobookFoldersUpdateTime() ?: 0L) > lastScanTime)
}


class ScannerViewModel @AssistedInject constructor(
    @Assisted initialState: ScannerViewState,
    private val configStore: ConfigStore,
) : MavericksViewModel<ScannerViewState>(initialState) {

    private var scannerLiveData: LiveData<WorkInfo>? = null
    private var workRequestId: UUID? = null

    init {
        configStore.getAudiobookFoldersUpdateTime()
            .map { it.toEpochMilli() }
            .execute(retainValue = ScannerViewState::audiobookFoldersUpdateTime) {
                copy(audiobookFoldersUpdateTime = it)
            }
    }

    private val observer = Observer<WorkInfo> { info ->
        if (info.state.isFinished) {
            setScanningFolders(false)

            val lastScanTime = Instant.now().toEpochMilli()
            setState { copy(lastScanTime = lastScanTime) }
        }
    }

    fun scanFolders(context: Context, force: Boolean = false, sourceId: SourceId? = null) =
        withState { state ->
            if (!force && !state.shouldScan) return@withState

            viewModelScope.launch {
                cancelScanFolders(context)
                setScanningFolders(true)

                val workRequest = BookScannerWorker.buildRequest(batchSize = 4, sourceId = sourceId)
                val workManager = WorkManager.getInstance(context)
                workManager.enqueue(workRequest)

                workRequestId = workRequest.id

                scannerLiveData = workManager.getWorkInfoByIdLiveData(workRequest.id).apply {
                    observeForever(observer)
                }
            }
        }

    fun cancelScanFolders(context: Context) {
        workRequestId?.let { WorkManager.getInstance(context).cancelWorkById(it) }
        workRequestId = null

        scannerLiveData?.removeObserver(observer)
        setScanningFolders(false)
    }

    private fun setScanningFolders(isScanningFolders: Boolean) {
        setState { copy(isScanningFolders = isScanningFolders) }
    }

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<ScannerViewModel, ScannerViewState> {
        override fun create(state: ScannerViewState): ScannerViewModel
    }

    companion object :
        MavericksViewModelFactory<ScannerViewModel, ScannerViewState> by hiltMavericksViewModelFactory()
}
