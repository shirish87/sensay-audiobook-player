package com.dotslashlabs.sensay.scan

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import data.entity.Book
import data.entity.BookWithChapters
import data.entity.Chapter
import data.entity.Source
import data.util.ContentDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import scanner.MediaScanner
import scanner.MediaScannerResult

object BookScanner {

    suspend fun scanSources(
        context: Context,
        sources: Collection<Source>,
        mediaScanner: MediaScanner,
        batchSize: Int = 4,
        bookFileFilter: suspend (file: DocumentFile) -> Boolean,
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
            val sourceBooks = mutableListOf<Pair<BookWithChapters, DocumentFile>>()
            var sourceBooksCount = 0

            val f = mediaScanner.scan(
                context,
                df,
                bookFileFilter,
            )

            f.collect { result ->
                sourceBooks.add(result.toBookWithChapters() to result.root)

                if (sourceBooks.size >= batchSize) {
                    sourceBooksCount += callback(sourceId, sourceBooks)
                    sourceBooks.clear()
                }
            }

            if (sourceBooks.isNotEmpty()) {
                sourceBooksCount += callback(sourceId, sourceBooks)
                sourceBooks.clear()
            }

            booksCount + sourceBooksCount
        }
    }
}

fun MediaScannerResult.toBookWithChapters() = BookWithChapters(
    book = Book(
        uri = root.uri,
        coverUri = coverUri,
        author = metadata.author,
        series = metadata.album,
        title = metadata.title,
        duration = ContentDuration(metadata.duration),
        hash = metadata.hash,
    ),
    chapters = chapters.map { c ->
        Chapter(
            uri = c.uri,
            coverUri = c.coverUri,
            hash = c.chapter.hash,
            trackId = c.chapter.id,
            title = c.chapter.title,
            start = ContentDuration(c.chapter.start),
            end = ContentDuration(c.chapter.end),
            duration = ContentDuration(c.chapter.duration),
        )
    }
)
