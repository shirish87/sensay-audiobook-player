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
import scanner.CoverScanner
import scanner.MediaScanner

object BookScanner {

    suspend fun scanSources(
        context: Context,
        sources: Collection<Source>,
        mediaScanner: MediaScanner,
        coverScanner: CoverScanner,
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

            mediaScanner.scan(
                context,
                listOf(df),
                bookFileFilter,
            ) { f, metadata ->
                val bookHash = metadata.hash
                val coverFile = coverScanner.scanCover(context, f.parentFile?.uri, f.uri, bookHash)

                sourceBooks.add(
                    BookWithChapters(
                        book = Book(
                            uri = f.uri,
                            author = metadata.author,
                            series = metadata.album,
                            title = metadata.title,
                            duration = ContentDuration(metadata.duration),
                            hash = bookHash,
                            coverUri = coverFile?.uri,
                        ),
                        chapters = metadata.chapters.map { chapter ->
                            Chapter(
                                uri = f.uri,
                                hash = chapter.hash,
                                trackId = chapter.id,
                                title = chapter.title,
                                start = ContentDuration(chapter.start),
                                end = ContentDuration(chapter.end),
                                duration = ContentDuration(chapter.duration),
                            )
                        }
                    ) to f
                )

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
