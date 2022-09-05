package scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.flow
import logcat.logcat
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

data class MediaScannerChapter(
    val uri: Uri,
    val coverUri: Uri?,
    val chapter: MetaDataChapter,
    val lastModified: Instant,
)

data class MediaScannerResult(
    val root: DocumentFile,
    val coverUri: Uri?,
    val metadata: Metadata,
    val chapters: List<MediaScannerChapter>,
) {

    val fileName: String = root.name!!.substringBeforeLast(".")

    companion object {

        fun create(
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
                    lastModified = Instant.ofEpochMilli(f.lastModified()),
                )
            }
        )
    }
}

internal data class LevelScan(
    val level: Int,
    val pendingFiles: MutableList<DocumentFile> = mutableListOf(),
    val leafDirs: MutableList<DocumentFile> = mutableListOf(),
)

class MediaScanner @Inject constructor(private val mediaAnalyzer: MediaAnalyzer) {

    private val coverScanner = CoverScanner()

    private val singleFileExtensions = listOf("m4a", "m4b", "mp4")

    private val supportedExtensions = singleFileExtensions +
            listOf("mp3", "mp2", "mp1", "ogg", "wav")

    private val validSupportedFileExtension = supportedExtensions.run {
        ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
    }

//    private val validSingleFileExtension = singleFileExtensions.run {
//        ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
//    }

    private val dirFilter = { f: DocumentFile ->
        f.isDirectory && f.canRead()
    }

    private val audioFileFilter = { f: DocumentFile ->
        f.isFile && f.canRead() && f.isNonEmptyFile() &&
                (f.type?.startsWith("audio/", true) == true ||
                        validSupportedFileExtension.matches(f.name!!))
    }

//    private val singleFileFilter = { f: DocumentFile ->
//        f.isFile && f.canRead() && validSingleFileExtension.matches(f.uri.toString())
//    }

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
        f.isDirectory && f.canRead() && f.listFiles()
            .run { !any(dirFilter) && any(audioFileFilter) }
    }

    suspend fun scan(
        context: Context,
        rootDir: DocumentFile,
        acceptFileFilter: suspend (file: DocumentFile) -> Boolean,
        maxLevels: Int = 4,
    ) = flow {
        if (!rootDir.isDirectory || !rootDir.canRead()) return@flow

        // (root) -> file.m4b
        // (root) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> (chapter) -> file.(m4b|mp3|...)

        val levels = (0 until maxLevels).fold(
            Pair(
                mutableMapOf<Int, LevelScan>(),
                rootDir.listFiles().toList(),
            ),
        ) { r, level ->

            if (r.second.isEmpty()) return@fold r

            val (acc, listing) = r
            val (audioFiles, filesOrDirs) = listing.partition(audioFileFilter)
            val (levelLeafDirs, nextLevel) = filesOrDirs.partition(leafDirFilter)

            acc.getOrPut(level) { LevelScan(level) }.apply {
                pendingFiles.addAll(audioFiles)
                leafDirs.addAll(levelLeafDirs)
            }

            Pair(
                acc,
                nextLevel.flatMap { l -> l.listFiles().toList() },
            )
        }.first.values.toList()

        levels.flatMap { (level, pendingFiles, levelDirs) ->
            logcat { "level=$level levelDirs=${levelDirs.joinToString { it.name ?: "" }} pendingFiles=${pendingFiles.size}" }

            if (levelDirs.isEmpty() && pendingFiles.isEmpty()) return@flatMap emptyList()

            levelDirs.flatMap { it.listFiles().filter(audioFileFilter) }
                .mapNotNull { f -> analyzeFile(context, f, acceptFileFilter) }
                .plus(pendingFiles.mapNotNull { f -> analyzeFile(context, f, acceptFileFilter) })
                .consolidate(::consolidateMediaScannerResults)
        }
            .groupBy { it.chapters.isNotEmpty() }
            .flatMap { (k, grp) ->
                if (k) return@flatMap grp

                // 0-chapter files in the same directory
                consolidateSingleMediaScannerResults(grp)
            }
            .filter { acceptFileFilter(it.root) }
            .also {
                logcat {
                    "levels processed books=${
                        it.joinToString { m ->
                            m.metadata.title ?: ""
                        }
                    } (${it.size})"
                }
            }
            .map { r -> emit(r) }
    }

    private fun consolidateSingleMediaScannerResults(
        results: Collection<MediaScannerResult>,
    ): List<MediaScannerResult> {

        return results.filter { it.root.isFile }
            .groupBy { it.root.parentNames().joinToString("/") }
            .mapNotNull { (grp, files) ->
                if (files.size <= 1) return@mapNotNull files.firstOrNull()

                logcat { "level=$grp files=${files.size}" }
                consolidateMediaScannerResults(files)
            }
    }

    private fun generateTags(r: MediaScannerResult, fallbackTitle: String): Map<String, String> =
        listOfNotNull(
            TagType.Title.name to (r.metadata.title ?: fallbackTitle),
            r.metadata.author?.let { TagType.Artist.name to it },
            r.metadata.album?.let { TagType.Album.name to it },
            r.metadata.albumAuthor?.let { TagType.AlbumArtist.name to it },
        ).toMap()

    private fun consolidateNonChapterizedSingleFiles(
        singleFiles: Collection<MediaScannerResult>,
    ): MediaScannerResult? {

        val sources: List<MediaScannerResult> = singleFiles
            .filter { it.chapters.isEmpty() }
            .sortedBy { it.root.name }

        if (sources.isEmpty()) {
            return null
        }

        val chapters = sources.foldIndexed(
            Duration.ZERO to mutableListOf<MediaScannerChapter>(),
        ) { fileIdx, (startTime, list), r ->

            val newChapter = MediaScannerChapter(
                uri = r.root.uri,
                coverUri = r.coverUri,
                lastModified = Instant.ofEpochMilli(r.root.lastModified()),
                chapter = MetaDataChapter(
                    id = fileIdx,
                    startTime = startTime.toDouble(DurationUnit.SECONDS),
                    endTime = (startTime + r.metadata.duration).toDouble(DurationUnit.SECONDS),
                    tags = generateTags(r, "${fileIdx + 1} - ${r.fileName}"),
                ),
            )

            list.add(newChapter)

            val nextStartTime = newChapter.chapter.end + 1.milliseconds
            nextStartTime to list
        }.second.toList()

        val baseBook = sources.first()
        val root = baseBook.root.parentFile!!
        val coverUri = chapters.firstOrNull { it.coverUri != null }?.coverUri

        logcat { "level consolidated-singles: book=${root.name} chapters=${chapters.size}" }
        return MediaScannerResult(
            root = root,
            coverUri = coverUri,
            chapters = chapters,
            metadata = baseBook.metadata.run {
                copy(
                    title = root.name,
                    duration = chapters.fold(Duration.ZERO) { acc, c -> acc + c.chapter.duration },
                    chapters = chapters.map { c -> c.chapter },
                    result = MetaDataScanResult(
                        streams = emptyList(),
                        chapters = emptyList(),
                        format = null,
                    ),
                )
            },
        )
    }

    private fun consolidateChapterizedSplitFiles(
        chapterized: Collection<MediaScannerResult>,
    ): MediaScannerResult {
        val chapters: List<MediaScannerChapter> = chapterized.foldIndexed(
            Duration.ZERO to mutableListOf<MediaScannerChapter>(),
        ) { fileIdx, (startTime, list), r ->

            val newChapters = r.chapters.sortedBy { it.chapter.id }
                .map { c ->
                    c.copy(
                        chapter = c.chapter.copy(
                            id = fileIdx + c.chapter.id,
                            startTime = startTime.toDouble(DurationUnit.SECONDS),
                            endTime = (startTime + c.chapter.duration).toDouble(DurationUnit.SECONDS),
                            tags = c.chapter.titleTag?.let { titleTag ->
                                c.chapter.tags!! + mapOf(
                                    titleTag to listOfNotNull(
                                        fileIdx + 1,
                                        c.chapter.title,
                                    ).joinToString(" - ")
                                )
                            } ?: c.chapter.tags,
                        )
                    )
                }

            list.addAll(newChapters)

            val nextStartTime = newChapters.last().chapter.end + 1.milliseconds
            nextStartTime to list
        }.second.toList()

        val firstBook = chapterized.first()

        val root = if (firstBook.root.isFile)
            firstBook.root.parentFile!!
        else firstBook.root

        val coverUri = chapterized.firstOrNull { it.coverUri != null }?.coverUri

        val metadata = firstBook.metadata.copy(
            duration = chapters.fold(Duration.ZERO) { acc, c -> acc + c.chapter.duration },
            chapters = chapters.map { c -> c.chapter },
            result = MetaDataScanResult(
                streams = emptyList(),
                chapters = emptyList(),
                format = null,
            ),
        )

        logcat { "level consolidated-chapterized: book=${metadata.title} chapters=${chapters.size}" }
        return MediaScannerResult(
            root = root,
            coverUri = coverUri,
            chapters = chapters,
            metadata = metadata,
        )
    }

    private fun consolidateMediaScannerResults(results: Collection<MediaScannerResult>): MediaScannerResult? {
        if (results.isEmpty()) return null

        // prefer using chapterized files (m4b) with 1 chapter per file
        val (chapterized, pendingFiles) = results.partition { it.chapters.isNotEmpty() }

        if (chapterized.isNotEmpty()) {
            return consolidateChapterizedSplitFiles(chapterized)
        }

        // merge files with 0 chapters per file
        if (pendingFiles.isNotEmpty()) {
            return consolidateNonChapterizedSingleFiles(pendingFiles)
        }

        return null
    }

    private suspend fun analyzeFile(
        context: Context,
        file: DocumentFile,
        acceptFileFilter: suspend (file: DocumentFile) -> Boolean,
    ): MediaScannerResult? {

        if (file.type?.startsWith("audio/", true) != true &&
            !validSupportedFileExtension.matches(file.name!!)
        ) return null

        if (!acceptFileFilter(file)) return null
        val metadata = mediaAnalyzer.analyze(context, file) ?: return null

        val coverFile = coverScanner.scanCover(
            context,
            file,
            metadata.hash,
        )

        return MediaScannerResult.create(file, coverFile, metadata)
    }
}

fun Collection<MediaScannerResult>.consolidate(
    consolidator: (Collection<MediaScannerResult>) -> MediaScannerResult?,
): List<MediaScannerResult> {

    return groupBy { r ->
        listOfNotNull(
            r.metadata.title ?: r.root.name,
            r.metadata.author ?: r.root.parentFile?.name,
            r.metadata.album,
        ).joinToString()
    }
        .flatMap { (k, grp) ->
            if (grp.size <= 1) return@flatMap grp

            val chapterized = grp.filter {
                it.chapters.size > 1 && it.metadata.duration > Duration.ZERO
            }.maxByOrNull { it.chapters.size }

            if (chapterized != null) {
                // if chapterized files exists, use file with most the chapters and ignore all others
                logcat { "FOUND level book=${k} USED CHAPTERIZED chapters=${chapterized.chapters.size}" }
                return@flatMap listOf(chapterized)
            }

            return@flatMap listOfNotNull(consolidator(grp))
        }
}


fun DocumentFile.parentNames(max: Int = 4) = sequence {
    var d: DocumentFile? = parentFile
    var i = 0

    while (i < max && d?.isDirectory == true && d.canRead()) {
        d.name?.let {
            yield(it)
            i++
        }

        d = d.parentFile
    }
}
