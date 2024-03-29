package com.dotslashlabs.sensay.scan

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.dotslashlabs.sensay.util.chunked
import data.entity.*
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import logcat.logcat
import scanner.MediaScanner
import scanner.MediaScannerResult
import java.time.Instant

object BookScanner {

    suspend fun scanSources(
        context: Context,
        sources: Collection<Source>,
        mediaScanner: MediaScanner,
        skipCoverScan: Boolean = true,
        acceptFileFilter: suspend (sourceId: SourceId, file: DocumentFile) -> Boolean = { _, _ -> true },
    ): Flow<Pair<SourceId, MediaScannerResult>> {

        return sources.asFlow().flatMapConcat { source ->
            val df = DocumentFile.fromTreeUri(context, source.uri)
            if (df == null || !df.isDirectory || !df.canRead()) return@flatMapConcat emptyFlow()

            mediaScanner.scan(
                context,
                df,
                { f -> acceptFileFilter(source.sourceId, f) },
                skipCoverScan,
            ).map { source.sourceId to it }
        }
    }

    private suspend fun scanSourcesOld(
        context: Context,
        sources: Collection<Source>,
        mediaScanner: MediaScanner,
        batchSize: Int = 4,
        skipCoverScan: Boolean = true,
        acceptFileFilter: suspend (file: DocumentFile) -> Boolean = { true },
        callback: suspend (
            sourceId: Long,
            booksWithChapters: List<Pair<BookWithChapters, DocumentFile>>,
        ) -> Int,
    ): Int = withContext(Dispatchers.IO) {

        if (sources.isEmpty()) {
            return@withContext 0
        }

        val sourceDocumentFileMap = sources.mapNotNull { source ->
            val df = DocumentFile.fromTreeUri(context, source.uri)
            if (df == null || !df.isDirectory || !df.canRead()) return@mapNotNull null

            source.sourceId to df
        }

        return@withContext sourceDocumentFileMap.fold(0) { booksCount, sourceDocumentFile ->
            val (sourceId, df) = sourceDocumentFile

            mediaScanner.scan(
                context,
                df,
                acceptFileFilter,
                skipCoverScan,
            )
                .chunked(batchSize)
                .fold(booksCount) { sourceBooksCount, results ->
                    logcat { "PROCESSING BATCH: sourceId=$sourceId ${results.size} ${results.map { it.root.name }}" }
                    val batch = results.map { it.toBookWithChapters() to it.root }
                    sourceBooksCount + callback(sourceId, batch)
                }
        }
    }
}

fun MediaScannerResult.toBookWithChapters() = BookWithChapters(
    book = Book(
        uri = root.uri,
        coverUri = coverUri,
        author = metadata.author,
        series = metadata.album,
        title = metadata.title ?: fileName,
        duration = ContentDuration(metadata.duration),
        hash = metadata.hash,
        lastModified = chapters.maxOfOrNull { it.lastModified } ?: Instant.now(),
    ),
    chapters = chapters.map { c ->
        Chapter(
            bookId = 0,
            uri = c.uri,
            coverUri = c.coverUri,
            hash = c.chapter.hash,
            trackId = c.chapter.id,
            title = c.chapter.title,
            author = c.chapter.artist ?: c.chapter.albumArtist,
            compilation = c.chapter.album,
            lastModified = c.lastModified,
            start = ContentDuration(c.chapter.start),
            end = ContentDuration(c.chapter.end),
            duration = ContentDuration(c.chapter.duration),
        )
    }
)
