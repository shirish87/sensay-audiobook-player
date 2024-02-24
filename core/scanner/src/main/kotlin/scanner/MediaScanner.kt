package scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logcat.logcat
import java.io.File
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
    val srcDirs: MutableList<DocumentFile> = mutableListOf(),

    val singleAudioFiles: MutableList<DocumentFile> = mutableListOf(),
    val audioFiles: MutableList<DocumentFile> = mutableListOf(),
    val levelDirs: MutableList<DocumentFile> = mutableListOf(),
)

class MediaScanner {

    private val coverScanner = CoverScanner()

    private val singleFileExtensions = listOf("m4a", "m4b", "mp4")

    private val supportedExtensions = singleFileExtensions +
        listOf("mp3", "mp2", "mp1", "ogg", "wav")

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
        f.isFile && f.canRead() && f.isNonEmptyFile() &&
            (
                f.type?.startsWith("audio/", true) == true ||
                    validSupportedFileExtension.matches(f.name!!)
                )
    }

    private val singleAudioFileFilter = { f: DocumentFile ->
        f.isFile && f.canRead() && f.isNonEmptyFile() && validSingleFileExtension.matches(f.name!!)
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
        f.isDirectory && f.canRead() && f.listFiles()
            .run { !any(dirFilter) && any(audioFileFilter) }
    }

    private suspend fun levelScan(
        dirFiles: List<DocumentFile>,
        channel: SendChannel<LevelScan>,
        level: Int = 0,
        maxLevels: Int = 4,
        chunkLevelDirSize: Int = 10,
        levelScan: LevelScan = LevelScan(level),
    ) {

        logcat { "levelScan: level=$level listing=${dirFiles.joinToString { it.name!! }}" }

        if (dirFiles.isEmpty() || level >= maxLevels) {
            logcat { "emit: $levelScan" }
            return channel.send(levelScan)
        }

        val nextLevelDirs = mutableListOf<DocumentFile>()

        dirFiles.forEach { l ->
            if (!l.isDirectory || !l.canRead()) return@forEach

            val listing = l.listFiles()
            val (singleAudios, filesOrDirs) = listing.partition(singleAudioFileFilter)
            val (audios, filesOrDirsMinusSingleAudio) = filesOrDirs.partition(audioFileFilter)
            val (dirs, nextLevel) = filesOrDirsMinusSingleAudio.partition(leafDirFilter)

            levelScan.apply {
                singleAudioFiles.addAll(singleAudios)
                audioFiles.addAll(audios)
                levelDirs.addAll(dirs)
            }

            nextLevelDirs.addAll(nextLevel)
        }

        if (levelScan.levelDirs.size >= chunkLevelDirSize) {
            logcat { "emit: single + audio files: $levelScan" }
            channel.send(levelScan.copy(levelDirs = mutableListOf()))

            levelScan.levelDirs.groupBy { o -> o.name?.get(0)?.toString() ?: "" }
                .forEach {
                    if (it.value.isEmpty()) return@forEach

                    logcat { "emit: alpha-(${it.key})-chunked levelDirs: $levelScan" }
                    channel.send(
                        levelScan.copy(
                            levelDirs = it.value.toMutableList(),
                            singleAudioFiles = mutableListOf(),
                            audioFiles = mutableListOf(),
                        )
                    )
                }
        } else {
            logcat { "emit: $levelScan" }
            channel.send(levelScan)
        }

        if (nextLevelDirs.isNotEmpty()) {
            levelScan(nextLevelDirs, channel,level + 1, maxLevels, chunkLevelDirSize)
        }
    }

    suspend fun scan(
        context: Context,
        rootDir: DocumentFile,
        acceptFileFilter: suspend (file: DocumentFile) -> Boolean,
        skipCoverScan: Boolean = true,
        maxLevels: Int = 4,
    ): Flow<MediaScannerResult> = channelFlow {
        if (!rootDir.isDirectory || !rootDir.canRead()) return@channelFlow

        // (root) -> file.m4b
        // (root) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> file.(m4b|mp3|...)
        // (root) -> (author|series) -> (book) -> (chapter) -> file.(m4b|mp3|...)

        runBlocking {
            val channel = Channel<LevelScan>()
            launch {
                levelScan(listOf(rootDir), channel, maxLevels = maxLevels)
                channel.close()
            }

            channel.consumeAsFlow().collect { scan ->
                val pendingFiles = scan.singleAudioFiles + scan.audioFiles

                scan.levelDirs.flatMap { it.listFiles().filter(audioFileFilter) }
                    .mapNotNull { f -> analyzeFile(context, f, acceptFileFilter, skipCoverScan) }
                    .plus(pendingFiles.mapNotNull { f ->
                        analyzeFile(context, f, acceptFileFilter, skipCoverScan)
                    })
                    .consolidate(::consolidateMediaScannerResults)
                    .groupBy { it.chapters.isNotEmpty() }
                    .flatMap { (hasChapters, grp) ->
                        if (hasChapters) return@flatMap grp

                        // 0-chapter files in the same directory
                        consolidateSingleMediaScannerResults(grp)
                    }
                    .filter { acceptFileFilter(it.root) }
                    /// .also { l -> logcat { "ADDING: ${l.joinToString { it.fileName }}" } }
                    .map { send(it) }
            }

            coroutineContext.cancelChildren()
        }
    }

    private fun consolidateSingleMediaScannerResults(
        results: Collection<MediaScannerResult>,
    ): List<MediaScannerResult> {

        return results.filter { it.root.isFile }
            .groupBy { it.root.parentNames().joinToString(File.separator) }
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
            mutableListOf<MediaScannerChapter>(),
        ) { fileIdx, list, r ->

            val newChapter = MediaScannerChapter(
                uri = r.root.uri,
                coverUri = r.coverUri,
                lastModified = Instant.ofEpochMilli(r.root.lastModified()),
                chapter = MetaDataChapter(
                    id = fileIdx,
                    startTime = 0.0,
                    endTime = r.metadata.duration.toDouble(DurationUnit.SECONDS),
                    tags = generateTags(r, "${fileIdx + 1} - ${r.fileName}"),
                ),
            )

            list.add(newChapter)
            list
        }.toList()

        val baseBook = sources.first()
        val root = baseBook.root.parentFile!!
        val coverUri = chapters.firstOrNull { it.coverUri != null }?.coverUri

        logcat {
            "level consolidated-singles: book=${root.name} chapters=${
            chapters.size
            } $chapters"
        }

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

    private fun consolidateChapterizedFiles(
        files: Collection<MediaScannerResult>,
    ): MediaScannerResult {

        var sequenceId = 0

        val chapters: List<MediaScannerChapter> = files
            .groupBy { it.root.uri }
            .toSortedMap()
            .flatMap { (_, chapterized) ->

                chapterized.fold(
                    Duration.ZERO to mutableListOf<MediaScannerChapter>(),
                ) { (startTime, list), r ->

                    val newChapters = r.chapters.sortedBy { it.chapter.id }
                        .map { c ->
                            sequenceId++

                            c.copy(
                                chapter = c.chapter.copy(
                                    id = sequenceId + c.chapter.id,
                                    startTime = startTime.toDouble(DurationUnit.SECONDS),
                                    endTime = (startTime + c.chapter.duration)
                                        .toDouble(DurationUnit.SECONDS),
                                    tags = c.chapter.titleTag?.let { titleTag ->
                                        c.chapter.tags!! + mapOf(
                                            titleTag to listOfNotNull(
                                                sequenceId,
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
            }

        val firstBook = files.first()

        val root = if (firstBook.root.isFile)
            firstBook.root.parentFile!!
        else firstBook.root

        val coverUri = files.firstOrNull { it.coverUri != null }?.coverUri

        val metadata = firstBook.metadata.copy(
            duration = chapters.fold(Duration.ZERO) { acc, c -> acc + c.chapter.duration },
            chapters = chapters.map { c -> c.chapter },
            result = MetaDataScanResult(
                streams = emptyList(),
                chapters = emptyList(),
                format = null,
            ),
        )

        logcat {
            "level consolidated-chapterized: book=${metadata.title} chapters=${
            chapters.size
            } $chapters"
        }

        return MediaScannerResult(
            root = root,
            coverUri = coverUri,
            chapters = chapters,
            metadata = metadata,
        )
    }

    private fun consolidateMediaScannerResults(
        results: Collection<MediaScannerResult>,
    ): MediaScannerResult? {

        if (results.isEmpty()) return null

        // prefer using chapterized files (m4b) with at least 1 chapter per file
        val (chapterized, pendingFiles) = results.partition { it.chapters.isNotEmpty() }

        if (chapterized.isNotEmpty()) {
            return consolidateChapterizedFiles(chapterized)
        }

        // merge files with 0 chapters per file
        if (pendingFiles.isNotEmpty()) {
            return consolidateNonChapterizedSingleFiles(pendingFiles)
        }

        return null
    }

    private suspend fun analyzeFile(
        @Suppress("UNUSED_PARAMETER") context: Context,
        file: DocumentFile,
        acceptFileFilter: suspend (file: DocumentFile) -> Boolean,
        @Suppress("UNUSED_PARAMETER") skipCoverScan: Boolean,
    ): MediaScannerResult? {

        if (file.type?.startsWith("audio/", true) != true &&
            !validSupportedFileExtension.matches(file.name!!)
        ) {
            // logcat { "analyzeFile.REJECTED: ${file.name} Failed mimetype or extension" }
            return null
        }

        if (!acceptFileFilter(file)) {
            // logcat { "analyzeFile.REJECTED: ${file.name} Failed acceptFileFilter" }
            return null
        }

//        val metadata = mediaAnalyzer.analyze(context, file)
//            ?: // logcat { "analyzeFile.REJECTED: ${file.name} Failed metadata analyzer" }
//            return null

        return null

//        val coverFile = if (skipCoverScan) null else coverScanner.scanCover(
//            context,
//            file,
//            metadata.hash,
//        )
//
//        // logcat { "analyzeFile.ACCEPTED: file=${file.name} / metadata=${metadata.duration} / ${metadata.chapters.size}" }
//        return MediaScannerResult.create(file, coverFile, metadata)
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
                logcat {
                    "FOUND level book=$k USED CHAPTERIZED chapters=${
                    chapterized.chapters.size
                    }"
                }

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
