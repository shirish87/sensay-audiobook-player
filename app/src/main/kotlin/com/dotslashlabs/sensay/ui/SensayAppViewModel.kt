package com.dotslashlabs.sensay.ui

import android.content.Context
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.airbnb.mvrx.*
import com.airbnb.mvrx.hilt.AssistedViewModelFactory
import com.airbnb.mvrx.hilt.hiltMavericksViewModelFactory
import com.dotslashlabs.sensay.scan.BookScannerWorker
import com.dotslashlabs.sensay.util.DevicePosture
import com.dotslashlabs.sensay.util.WindowSizeClass
import config.ConfigStore
import config.HomeLayout
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import data.SensayStore
import data.entity.BookProgressWithBookAndChapters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*

val DEFAULT_HOME_LAYOUT = HomeLayout.LIST

data class SensayAppState(
    val books: Async<List<BookProgressWithBookAndChapters>> = Uninitialized,
    val homeLayout: HomeLayout = DEFAULT_HOME_LAYOUT,
    val isScanningFolders: Boolean = false,
    val audiobookFoldersUpdateTime: Async<Long?> = Uninitialized,
    val lastScanTime: Long = Instant.EPOCH.toEpochMilli(),
    val windowSize: WindowSizeClass = WindowSizeClass.default(),
    val devicePosture: DevicePosture = DevicePosture.default(),
) : MavericksState {

    val shouldScan = ((audiobookFoldersUpdateTime() ?: 0L) > lastScanTime)

    val useLandscapeLayout = (windowSize.heightSizeClass == WindowHeightSizeClass.Compact)
}

class SensayAppViewModel @AssistedInject constructor(
    @Assisted private val state: SensayAppState,
    store: SensayStore,
    private val configStore: ConfigStore,
) : MavericksViewModel<SensayAppState>(state) {

    private var scannerLiveData : LiveData<WorkInfo>? = null
    private var workRequestId: UUID? = null

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

    private val observer = Observer<WorkInfo> { info ->
        if (info.state.isFinished) {
            setScanningFolders(false)
            setState { copy(lastScanTime = Instant.now().toEpochMilli()) }
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            scannerLiveData?.removeObserver(observer)
        }

        super.onCleared()
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

    fun scanFolders(context: Context, force: Boolean = false) = withState { state ->
        if (!force && !state.shouldScan) return@withState

        viewModelScope.launch {
            cancelScanFolders(context)
            setScanningFolders(true)

            val workRequest = BookScannerWorker.buildRequest(batchSize = 4)
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

    suspend fun getLastPlayedBookId() = configStore.getLastPlayedBookId().first()

    @AssistedFactory
    interface Factory : AssistedViewModelFactory<SensayAppViewModel, SensayAppState> {
        override fun create(state: SensayAppState): SensayAppViewModel
    }

    companion object : MavericksViewModelFactory<SensayAppViewModel, SensayAppState>
    by hiltMavericksViewModelFactory()
}
