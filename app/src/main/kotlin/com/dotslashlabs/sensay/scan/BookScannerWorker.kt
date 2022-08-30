package com.dotslashlabs.sensay.scan

import android.app.Notification
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import data.SensayStore
import kotlinx.coroutines.flow.firstOrNull
import scanner.CoverScanner
import scanner.MediaScanner

@HiltWorker
class BookScannerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val store: SensayStore,
    private val mediaScanner: MediaScanner,
    private val coverScanner: CoverScanner,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_BATCH_SIZE = "BATCH_SIZE"
        const val RESULT_BOOKS_ADDED_COUNT = "BOOKS_ADDED_COUNT"

        private const val DEFAULT_BATCH_SIZE = 4

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL = "BookScannerWorker"

        fun buildRequest(batchSize: Int) = OneTimeWorkRequestBuilder<BookScannerWorker>()
            .setInputData(workDataOf(KEY_BATCH_SIZE to batchSize))
            .build()
    }

    override suspend fun doWork(): Result {
        val activeSources = (store.sources(isActive = true).firstOrNull() ?: emptyList())
            .sortedBy { -it.createdAt.toEpochMilli() }

        val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)

        val booksAddedCount = BookScanner.scanSources(
            applicationContext,
            activeSources,
            mediaScanner,
            coverScanner,
            batchSize,
            bookFileFilter = { file -> (store.bookByUri(file.uri).firstOrNull() == null) },
        ) { sourceId, booksWithChapters ->

            store.createBooksWithChapters(
                sourceId,
                booksWithChapters
            ).size
        }

        return Result.success(
            workDataOf(RESULT_BOOKS_ADDED_COUNT to booksAddedCount),
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            Notification.Builder(applicationContext, NOTIFICATION_CHANNEL)
                .setContentTitle("Scanning")
                .setContentText("")
                .build(),
        )
    }
}
