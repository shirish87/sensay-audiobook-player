package com.dotslashlabs.sensay.scan

import android.app.Notification
import android.content.Context
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.dotslashlabs.sensay.util.chunked
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import data.BookWithChaptersAndTags
import data.SensayStore
import data.entity.BookId
import data.entity.BookWithChapters
import data.entity.Source
import data.entity.SourceId
import kotlinx.coroutines.flow.*
import logcat.logcat
import scanner.CoverScanner
import scanner.MediaScanner
import scanner.MediaScannerResult
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.time.Instant
import java.util.regex.Pattern

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
        const val KEY_SOURCE_ID = "SOURCE_ID"
        const val RESULT_BOOKS_ADDED_COUNT = "BOOKS_ADDED_COUNT"

        private const val DEFAULT_BATCH_SIZE = 4

        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL = "BookScannerWorker"

        fun buildRequest(
            batchSize: Int,
            sourceId: Long? = null,
        ) = OneTimeWorkRequestBuilder<BookScannerWorker>()
            .setInputData(
                workDataOf(
                    *listOfNotNull(
                        KEY_BATCH_SIZE to batchSize,
                        sourceId?.let { KEY_SOURCE_ID to it },
                    ).toTypedArray(),
                ),
            )
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
        val inputSourceId = inputData.getLong(KEY_SOURCE_ID, 0L)

        val activeSources = if (inputSourceId > 0) {
            listOfNotNull(store.sourceById(inputSourceId).firstOrNull())
        } else (store.sources(isActive = true).firstOrNull() ?: emptyList())
            .sortedBy { -it.createdAt.toEpochMilli() }

        // val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
        val scanInstant = Instant.now()

        var booksAddedCount: Int = 0

        activeSources.asFlow()
            .flatMapConcat { source ->
                scanSource(source)
                    .onStart {
                        logcat { "SCAN-COLLECT: sourceId=${source.sourceId} STARTED" }
                        store.startSourceScan(source.sourceId)
                    }
                    .onCompletion {err ->
                        logcat { "SCAN-COLLECT: sourceId=${source.sourceId} ENDED ${err?.message ?: "WITHOUT ERROR"}" }
                        store.endSourceScan(source.sourceId)
                    }
            }
            .collect { (sourceId, bookData, existingBookId) ->

                val bookWithChapters = if (bookData.first.book.coverUri != null) {
                    bookData.first
                } else {
                    val (bookWithChaptersNoCover, mediaScannerResult) = bookData

                    val coverUri = coverScanner.scanCover(
                        applicationContext,
                        mediaScannerResult.root,
                        bookWithChaptersNoCover.book.hash,
                    )?.uri

                    bookWithChaptersNoCover.copy(
                        book = bookWithChaptersNoCover.book.copy(coverUri = coverUri)
                    )
                }

                val bookId: BookId? = if (existingBookId == null) {
                    val bookIds = store.createOrUpdateBooksWithChapters(
                        sourceId,
                        listOf(
                            BookWithChaptersAndTags(
                                bookWithChapters = bookWithChapters,
                                tags = emptySet(),
                            ),
                        ),
                        scanInstant,
                    )

                    if (bookIds.isNotEmpty()) {
                        booksAddedCount += bookIds.size
                        logcat { "SCAN-COLLECT: sourceId=$sourceId result=${bookWithChapters.book.title} ACCEPT" }
                        bookIds[0]
                    } else {
                        logcat { "SCAN-COLLECT: sourceId=$sourceId result=${bookWithChapters.book.title} ACCEPT-FAILED" }
                        null
                    }
                } else {
                    logcat { "SCAN-COLLECT: sourceId=$sourceId result=${bookWithChapters.book.title} REJECT existingBookId=$existingBookId" }

                    // check cover
                    bookWithChapters.book.coverUri?.let { coverUri ->
                        val book = store.bookById(existingBookId).firstOrNull()
                        if (book != null && book.coverUri == null) {
                            store.updateBook(book.bookId, coverUri)
                            logcat { "SCAN-COLLECT: sourceId=$sourceId result=${bookWithChapters.book.title} UPDATE existingBookId=$existingBookId" }
                        }
                    }

                    existingBookId
                }

                if (bookId != null) {
                    store.updateSourceBook(
                        sourceId,
                        bookId,
                        isActive = true,
                        inactiveReason = null,
                    )
                }
            }

        return Result.success(
            workDataOf(RESULT_BOOKS_ADDED_COUNT to booksAddedCount),
        )
    }

    private suspend fun scanSource(source: Source): Flow<Triple<SourceId, Pair<BookWithChapters, MediaScannerResult>, BookId?>> {
        val sourceBooks = store.bookSourceScansWithBooks(source.sourceId).firstOrNull()
            ?: emptyList()

        val sourceBooksByUri = sourceBooks.associateBy { it.book.uri }

        return BookScanner.scanSources(
            applicationContext,
            listOf(source),
            mediaScanner,
            skipCoverScan = true,
        ).transform { (sourceId, mediaScannerResult) ->
            val bookWithChapters = mediaScannerResult.toBookWithChapters()
            val book = bookWithChapters.book

            book.lastModified?.let { lastModified ->
                val lastKnownBook = sourceBooksByUri[book.uri]?.book
                    // ?: sourceBooksByHash[book.hash]?.book

                if (lastKnownBook != null) {
                    if (lastKnownBook.duration.ms == book.duration.ms) {
                        logcat { "SKIPPED: Found '${book.title}' with same duration ${lastKnownBook.uri} vs (${book.uri})" }
                        return@transform emit(Triple(sourceId, bookWithChapters to mediaScannerResult, lastKnownBook.bookId))
                    }

                    if (lastModified <= lastKnownBook.lastModified) {
                        logcat { "SKIPPED: Found '${book.title}' not newer than existing by uri/hash" }
                        return@transform emit(Triple(sourceId, bookWithChapters to mediaScannerResult, lastKnownBook.bookId))
                    }
                }
            }

            return@transform emit(Triple(sourceId, bookWithChapters to mediaScannerResult, null))
        }
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
