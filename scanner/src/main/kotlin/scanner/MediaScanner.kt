package scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.flow
import logcat.LogPriority
import logcat.logcat
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

data class MediaScannerChapter(
    val uri: Uri,
    val coverUri: Uri?,
    val chapter: MetaDataChapter,
)

data class MediaScannerResult(
    val root: DocumentFile,
    val coverUri: Uri?,
    val metadata: Metadata,
    val chapters: List<MediaScannerChapter>
) {

    companion object {

        fun fromSingleFileMetadata(
            f: DocumentFile,
            coverFile: DocumentFile?,
            metadata: Metadata,
        ) = MediaScannerResult(
            root = f,
            coverUri = coverFile?.uri,
            metadata = metadata,
            chapters = metadata.chapters.map {
                MediaScannerChapter(
                    uri = f.uri,
                    coverUri = coverFile?.uri,
                    chapter = it,
                )
            }
        )
    }
}

class MediaScanner @Inject constructor(private val mediaAnalyzer: MediaAnalyzer) {

    private val coverScanner = CoverScanner()

    private val supportedExtensions = listOf("m4a", "m4b", "mp4", "mp3", "mp2", "mp1", "ogg", "wav")
    private val singleFileExtensions = listOf("m4a", "m4b", "mp4")

    private val validSupportedFileExtension = supportedExtensions.run {
        ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
    }

    private val validSingleFileExtension = singleFileExtensions.run {
        ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
    }

    private val dirFilter = { f: DocumentFile ->
        f.isDirectory && f.canRead()
    }

    private val audioFileFilter = { f: DocumentFile ->
        f.isFile && f.canRead() && (f.type?.startsWith("audio/", true) == true ||
                validSupportedFileExtension.matches(f.uri.toString()))
    }

    private val singleFileFilter = { f: DocumentFile ->
        f.isFile && f.canRead() && validSingleFileExtension.matches(f.uri.toString())
    }

//    private val nonLeafDirFilter = { f: DocumentFile ->
//        // current f is a readable directory
//        // not containing any supported files
//        // and contains at least one directory
//        f.isDirectory && f.canRead() && f.listFiles().run { !any(audioFileFilter) && any(dirFilter) }
//    }

    private val leafDirFilter = { f: DocumentFile ->
        // current f is a readable directory
        // not containing any directory
        // and contains at least one supported file
        f.isDirectory && f.canRead() && f.listFiles().run { !any(dirFilter) && any(audioFileFilter) }
    }

    suspend fun scan(
        context: Context,
        rootDir: DocumentFile,
        existingFileFilter: suspend (file: DocumentFile) -> Boolean,
        maxLevels: Int = 4,
    ) = flow {
        if (!rootDir.isDirectory || !rootDir.canRead()) return@flow

        // (root) -> file.m4b
        // (root) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> (chapter) -> file.(m4b|mp3|...)

        val (levels, _) = (0 until maxLevels).fold(
            Triple(
                mutableListOf<List<DocumentFile>>(),
                mutableSetOf<Uri>(),
                rootDir.listFiles().toList(),
            ),
        ) { (acc, result, listing), _ ->

            if (listing.isEmpty()) return@fold Triple(acc, result, listing)

            val (singleFiles, filesOrDirs) = listing.partition(singleFileFilter)

            singleFiles.forEach { f ->
                // (root) -> file.m4b
                analyzeFile(context, f, existingFileFilter)?.let {
                    result.add(f.uri)
                    emit(it)
                }
            }

            val (levelLeafDirs, levelPending) = filesOrDirs.partition(leafDirFilter)
            if (levelLeafDirs.isNotEmpty()) {
                acc.add(levelLeafDirs)
            }

            Triple(acc, result, levelPending.flatMap { l -> l.listFiles().toList() })
        }

        levels.flatMapIndexed { depth, levelDirs ->
            levelDirs.mapNotNull { d -> processLevelLeafDir(context, d, existingFileFilter, depth) }
                .groupBy { it.metadata.title }
                .flatMap { (_, results) ->
                    combineMediaScannerResults(results)?.let { listOf(it) } ?: emptyList()
                }
        }
        .map { r -> emit(r) }
    }

    private suspend fun processLevelLeafDir(
        context: Context,
        leafDir: DocumentFile,
        existingFileFilter: suspend (file: DocumentFile) -> Boolean,
        @Suppress("UNUSED_PARAMETER") depth: Int,
    ): MediaScannerResult? {

        val pathSegments = (0..depth).fold(leafDir to mutableListOf<String>()) { (l, r), _ ->
            r.add(l.name!!)
            l.parentFile!! to r
        }.second.reversed()

        val leafFiles = leafDir.listFiles().filter(audioFileFilter)

        // directory contains multiple files
        // scan metadata for all
        val leafFilesWithMetadata = leafFiles.mapNotNull l@{ f ->
            val m = analyzeFile(context, f, existingFileFilter) ?: return@l null
            f to m
        }.sortedBy { (f) -> f.name }

        if (
            leafFilesWithMetadata.all { (_, m) ->
                m.metadata.chapters.isEmpty() && m.metadata.duration > Duration.ZERO
            }
        ) {

            var startTime = 0.0

            val chapters: List<MediaScannerChapter> =
                leafFilesWithMetadata.mapIndexed { idx, (f, r) ->
                    val endTime = startTime + r.metadata.duration.inWholeSeconds

                    val chapter = MediaScannerChapter(
                        uri = r.root.uri,
                        coverUri = r.coverUri,
                        chapter = MetaDataChapter.create(
                            id = idx,
                            title = r.metadata.title.ifEmpty {
                                f.name?.substringBeforeLast(".")
                                    ?: "Chapter ${idx + 1}"
                            },
                            artist = r.metadata.author,
                            album = r.metadata.album,
                            startTime = startTime,
                            endTime = endTime,
                        ),
                    )

                    startTime = endTime + 1
                    chapter
                }

            val bookDuration = chapters.fold(Duration.ZERO) { acc, c ->
                acc + c.chapter.duration
            }

            val bookTitle = leafFilesWithMetadata.firstOrNull {
                it.second.metadata.album?.isNotEmpty() == true
            }?.second?.metadata?.album ?: leafDir.name!!

            return MediaScannerResult(
                root = leafDir,
                coverUri = null,
                chapters = chapters,
                metadata = Metadata(
                    title = bookTitle,
                    duration = bookDuration,
                    author = null,
                    album = null,
                    chapters = emptyList(),
                    result = MetaDataScanResult(
                        streams = emptyList(),
                        chapters = emptyList(),
                        format = null,
                    ),
                ),
            )
        }

        leafFilesWithMetadata.forEach {
            logcat(LogPriority.WARN) { "Skipped: ${it.first.name} (${pathSegments.joinToString(" > ")})" }
        }

        return null
    }

    private fun combineMediaScannerResults(results: List<MediaScannerResult>): MediaScannerResult? {
        if (results.size <= 1) {
            return results.firstOrNull()
        }

        // merge books with same name
        logcat { "COLLATE: book=${results.first().metadata.title} chapters=${results.size}" }
        var startTime = Duration.ZERO

        val chapters: List<MediaScannerChapter> =
            results.flatMapIndexed { bookIdx, b ->
                val offsetIdx = bookIdx * (if (bookIdx > 0)
                    results[bookIdx - 1].chapters.size + 1
                else 0)

                b.chapters.mapIndexed { chapterIdx, r ->
                    val endTime = startTime + r.chapter.duration

                    val chapter = MediaScannerChapter(
                        uri = r.uri,
                        coverUri = r.coverUri,
                        chapter = r.chapter.copy(
                            id = offsetIdx + chapterIdx,
                            startTime = startTime.toDouble(DurationUnit.SECONDS),
                            endTime = endTime.toDouble(DurationUnit.SECONDS),
                        ),
                    )

                    startTime = endTime + 1.milliseconds
                    chapter
                }
            }

        val bookDuration = chapters.fold(Duration.ZERO) { acc, c ->
            acc + c.chapter.duration
        }

        return results.first().run {
            copy(
                chapters = chapters,
                metadata = metadata.copy(
                    duration = bookDuration,
                )
            )
        }
    }

    private suspend fun analyzeFile(
        context: Context,
        file: DocumentFile,
        existingFileFilter: suspend (file: DocumentFile) -> Boolean,
    ): MediaScannerResult? {
        if (file.type?.startsWith("audio/", true) != true &&
            !validSupportedFileExtension.matches(file.uri.toString())
        ) return null

        if (!existingFileFilter(file)) return null
        val metadata = mediaAnalyzer.analyze(context, file.uri) ?: return null

        val coverFile = coverScanner.scanCover(
            context,
            file.parentFile?.uri,
            file.uri,
            metadata.hash,
        )

        return MediaScannerResult.fromSingleFileMetadata(file, coverFile, metadata)
    }
}
