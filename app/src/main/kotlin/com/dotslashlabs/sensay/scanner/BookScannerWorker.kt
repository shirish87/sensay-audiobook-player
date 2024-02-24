package com.dotslashlabs.sensay.scanner

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dotslashlabs.media3.extractor.m4b.metadata.ChapterMetadata
import com.dotslashlabs.media3.extractor.m4b.metadata.M4bMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import data.SensayStore
import data.SourceBookWithChaptersAndTags
import data.entity.Book
import data.entity.BookSourceScanWithBook
import data.entity.Chapter
import data.entity.Source
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import logcat.logcat
import media.DocumentScanner
import media.SourceFile
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.microseconds

@HiltWorker
class BookScannerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val store: SensayStore,
    private val documentScanner: DocumentScanner,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val KEY_BATCH_SIZE = "BATCH_SIZE"
        const val KEY_SOURCE_ID = "SOURCE_ID"
        const val RESULT_BOOKS_ADDED_COUNT = "BOOKS_ADDED_COUNT"
        const val RESULT_BOOKS_REJECTED_COUNT = "BOOKS_REJECTED_COUNT"

//        private const val DEFAULT_BATCH_SIZE = 4

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun doWork(): Result {
        val inputSourceId = inputData.getLong(KEY_SOURCE_ID, 0L)

        val activeSources = (if (inputSourceId > 0) {
            listOfNotNull(store.sourceById(inputSourceId).firstOrNull())
        } else (store.sources(isActive = true).firstOrNull()
            ?: emptyList())).filter { !it.isScanning }.sortedBy { -it.createdAt.toEpochMilli() }

        // val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)
        val scanInstant = Instant.now()
        var booksAddedCount = 0
        var booksRejectedCount = 0

        val coversFolder = withContext(Dispatchers.IO) {
            File(applicationContext.cacheDir, "covers").also {
                if (!it.exists()) {
                    it.mkdirs()
                }
            }
        }

        activeSources.asFlow()
            .flatMapConcat { source ->
                logcat { "SCAN-COLLECT: sourceId=${source.sourceId} STARTED isScanning=${source.isScanning}" }
                store.startSourceScan(source.sourceId)

                scanSource(source, coversFolder, scanInstant)
                    .mapNotNull { (r, needsUpdate) ->
                        val sourceId = r.sourceId
                        val book = r.bookWithChapters.book

                        if (needsUpdate) {
                            // either new book or existing book with duration mismatch
                            val isNewBook = (book.bookId == 0L)

                            val bookIds = store.createOrUpdateBooksWithChapters(
                                r.sourceId,
                                booksWithChaptersAndTags = listOf(r),
                                scanInstant,
                            )

                            if (bookIds.isEmpty()) {
                                booksRejectedCount++
                                logcat { "SCAN-COLLECT: sourceId=$sourceId $isNewBook result=${book.title} FAILED" }
                                return@mapNotNull null
                            }

                            booksAddedCount += bookIds.size
                            logcat { "SCAN-COLLECT: sourceId=$sourceId $isNewBook result=${book.title} ACCEPT" }
                            return@mapNotNull r.sourceId to bookIds[0]
                        } else {
                            logcat { "SCAN-COLLECT: sourceId=$sourceId result=${book.title} OK" }
                            r.sourceId to book.bookId
                        }
                    }
                    .onCompletion { err ->
                        val summary = "added=$booksAddedCount rejected=$booksRejectedCount"
                        logcat { "SCAN-COLLECT: sourceId=${source.sourceId} ENDED ${err?.message ?: "WITHOUT ERROR"} ($summary)" }
                        store.endSourceScan(source.sourceId)
                    }
            }
            .collect { (sourceId, bookId) ->
                store.updateSourceBook(
                    sourceId,
                    bookId,
                    isActive = true,
                    inactiveReason = null,
                )
            }

        return Result.success(
            workDataOf(
                RESULT_BOOKS_ADDED_COUNT to booksAddedCount,
                RESULT_BOOKS_REJECTED_COUNT to booksRejectedCount,
            ),
        )
    }

    private suspend fun scanSource(
        source: Source,
        coversFolder: File,
        scanInstant: Instant = Instant.now(),
    ): Flow<Pair<SourceBookWithChaptersAndTags, Boolean>> {
        val existingBooksByHash = store.bookSourceScansWithBooks(source.sourceId).firstOrNull()
            ?.associateBy { it.book.hash }
            ?: emptyMap()

        val (imageCompressFormat, imageQuality) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY to 85
        } else {
            Bitmap.CompressFormat.JPEG to 90
        }

        return documentScanner.scanSource(source, scanInstant)
            .transform { r ->
                val title = "${r.metadata.albumTitle ?: r.metadata.title}"
                val duration = ContentDuration.ms(
                    r.metadata.durationUs?.microseconds?.inWholeMilliseconds ?: 0
                )

                val existingBook = existingBooksByHash[r.hash]
                if (existingBook == null || existingBook.book.duration != duration || !compareChapters(
                        r.metadata,
                        existingBook.chapters,
                        r.chapters,
                    )
                ) {
                    // new book or existing book with duration mismatch or source/chapters mismatch
                    val coverMap: Map<String, Pair<Uri, Int>> = buildCoverMap(
                        r,
                        existingBook,
                        coversFolder,
                        imageCompressFormat = imageCompressFormat,
                        imageQuality = imageQuality,
                    )

                    logcat { "$coverMap" }

                    emit(
                        r.toBookWithChaptersAndTags(
                            existingBook?.book?.bookId ?: 0,
                            coverMap
                        ) to true
                    )
                } else {
                    // existing book based on hash + duration + chapters match
                    logcat { "SKIPPED: Found '${title}' with same duration" }
                    emit(
                        r.toBookWithChaptersAndTags(
                            existingBook.book.bookId,
                            emptyMap(),
                        ) to false
                    )
                }
            }
    }

    private fun compareChapters(
        bookMetadata: M4bMetadata,
        chapters: List<Chapter>,
        scannedChapters: List<Triple<Uri, ChapterMetadata, M4bMetadata?>>,
    ): Boolean {
        return (chapters.size == scannedChapters.size && chapters.indices.all { idx ->
            val c = chapters[idx]
            val s = scannedChapters[idx]
            val scannedArtworkSize = s.third?.artworkData?.size ?: bookMetadata.artworkData?.size ?: 0

            c.uri == s.first && c.title == s.second.chapterTitle && (scannedArtworkSize == 0 || c.srcCoverByteSize == scannedArtworkSize)
        })
    }

    private suspend fun buildCoverMap(
        r: SourceFile,
        existingBook: BookSourceScanWithBook?,
        coversFolder: File,
        bookKeyedBy: (Book) -> String = { it.hash },
        chaptersKeyedBy: (String, Int) -> String = { hash, idx -> "$hash-$idx" },
        imageCompressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        imageQuality: Int = 100,
    ): Map<String, Pair<Uri, Int>> = withContext(Dispatchers.IO) {

        // conditional check for API level before using constants
        val imageExtension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (imageCompressFormat) {
                Bitmap.CompressFormat.PNG -> "png"
                Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
                Bitmap.CompressFormat.WEBP_LOSSY -> "webp"
                else -> "jpg"
            }
        } else {
            when (imageCompressFormat) {
                Bitmap.CompressFormat.PNG -> "png"
                else -> "jpg"
            }
        }

        val bookCoverBytes: ByteArray? = r.metadata.artworkData
            ?: r.chapters.firstNotNullOfOrNull { it.third?.artworkData }

        val bookHash = existingBook?.book?.hash ?: r.hash

        val bookCover: Pair<Uri?, Int?> = if (existingBook?.chapter?.coverUri?.toFile()?.exists() == true) {
            existingBook.chapter?.coverUri to existingBook.chapter?.srcCoverByteSize
        } else {
            bookCoverBytes?.let {
                var bmp: Bitmap? = null

                try {
                    bmp = BitmapFactory.decodeStream(ByteArrayInputStream(it))

                    val coverUri = File(coversFolder, "${bookHash}.$imageExtension").apply {
                        outputStream().use { os ->
                            bmp.compress(imageCompressFormat, imageQuality, os)
                        }
                    }.toUri()

                    logcat { "bookCoverUri created: $coverUri" }
                    coverUri to bookCoverBytes.size
                } finally {
                    bmp?.recycle()
                }
            } ?: (null to null)
        }

        val (bookCoverUri, bookCoverSize) = bookCover

        if (r.chapters.map { it.first }.toSet().size == 1 && bookCoverUri != null) {
            // only 1 chapter exists and book cover is available
            // same source file for all chapters, use a single cover image

            return@withContext List(r.chapters.size) { idx ->
                chaptersKeyedBy(bookHash, idx) to (bookCoverUri to (bookCoverSize ?: 0))
            }.toMap()
        }

        val chapterMap: Map<String, Pair<Uri, Int>> = existingBook?.chapters?.mapNotNull {
            it.coverUri?.let { uri ->
                chaptersKeyedBy(bookHash, it.trackId) to (uri to (it.srcCoverByteSize ?: 0))
            }
        }?.toMap() ?: emptyMap()

        val coverMap: Map<String, Pair<Uri, Int>> = if (bookCoverUri != null) {
            chapterMap + (if (existingBook != null) {
                bookKeyedBy(existingBook.book) to (bookCoverUri to (bookCoverSize ?: 0))
            } else bookHash to (bookCoverUri to (bookCoverSize ?: 0)))
        } else {
            chapterMap
        }

        return@withContext List(r.chapters.size) { idx ->
            val key = chaptersKeyedBy(bookHash, idx)

            coverMap[key]?.let { (uri, coverSize) ->
                if (uri.toFile().exists()) {
                    logcat { "coverUri exists: $key -> $uri" }
                    return@List key to (uri to coverSize)
                }
            }

            r.chapters[idx].third?.artworkData?.let {
                // chapter file has cover image
                val image = BitmapFactory.decodeStream(ByteArrayInputStream(it))
                val uri = File(coversFolder, "$key.$imageExtension").apply {
                    outputStream().use { os ->
                        image.compress(imageCompressFormat, imageQuality, os)
                    }
                }.toUri().also { image.recycle() }

                return@List key to (uri to it.size)
            }

            if (bookCoverUri == null) return@List null
            key to (bookCoverUri to (bookCoverSize ?: 0))
        }.filterNotNull().toMap()
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
