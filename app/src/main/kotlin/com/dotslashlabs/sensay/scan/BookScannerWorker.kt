package com.dotslashlabs.sensay.scan

import android.app.Notification
import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import data.BookWithChaptersAndTags
import data.SensayStore
import kotlinx.coroutines.flow.firstOrNull
import scanner.CoverScanner
import scanner.MediaScanner
import java.util.regex.Pattern

@HiltWorker
class BookScannerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val store: SensayStore,
    private val mediaScanner: MediaScanner,
    @Suppress("UNUSED_PARAMETER") private val coverScanner: CoverScanner,
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

        private val excludeWords = listOf(
            "^audiobook.*",
            "^primary:.*",
            "^document$",
            "^tree$",
        ).map { s -> s.toPattern(flags = Pattern.CASE_INSENSITIVE) }

        private val tagFilter: (String) -> Boolean = { str ->
            excludeWords.any { it.matcher(str).matches() }
        }
    }

    override suspend fun doWork(): Result {
        val activeSources = (store.sources(isActive = true).firstOrNull() ?: emptyList())
            .sortedBy { -it.createdAt.toEpochMilli() }

        val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)

        val booksAddedCount = BookScanner.scanSources(
            applicationContext,
            activeSources,
            mediaScanner,
            batchSize,
            bookFileFilter = { file -> (store.bookByUri(file.uri).firstOrNull() == null) },
        ) { sourceId, sourceBooks ->

            store.createBooksWithChapters(
                sourceId,
                sourceBooks.map { (booksWithChapters, f) ->
                    BookWithChaptersAndTags(
                        booksWithChapters = booksWithChapters,
                        tags = getTags(f),
                    )
                },
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

    private fun getTags(f: DocumentFile?, maxDepth: Int = 3): Set<String> {
        if (f == null) return emptySet()
        if (f.isFile) return getTags(f.parentFile, maxDepth)

        var fileDir: DocumentFile? = f

        return (0 until maxDepth).mapNotNull {
            val name = fileDir?.name ?: return@mapNotNull null
            fileDir = fileDir?.parentFile

            if (tagFilter(name)) null else name
        }.toSortedSet()
    }
}