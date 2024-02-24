package media

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dotslashlabs.media3.extractor.m4b.M4bExtractor
import com.dotslashlabs.media3.extractor.m4b.metadata.ChapterMetadata
import com.dotslashlabs.media3.extractor.m4b.metadata.M4bMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import data.SensayStore
import data.entity.Source
import data.entity.SourceId
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import logcat.logcat
import java.io.File
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class DocumentScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SensayStore,
) {

    companion object {
        private val singleFileExtensions = listOf("m4a", "m4b", "mp4")

        private val supportedExtensions = singleFileExtensions +
                listOf("mp3", "mp2", "mp1", "ogg", "wav")

        private val validSupportedFileExtension = supportedExtensions.run {
            ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
        }

        private val validSingleFileExtension = singleFileExtensions.run {
            ".*\\.(${joinToString("|")})$".toRegex(RegexOption.IGNORE_CASE)
        }

        private val dirFilter = { f: DocumentFile -> f.isDirectory && f.canRead() }

        private val audioFileFilter = { f: DocumentFile ->
            f.isFile && f.canRead() && f.isNonEmptyFile() && (f.type?.startsWith(
                "audio/",
                true
            ) == true || validSupportedFileExtension.matches(f.name!!))
        }

        private val singleAudioFileFilter = { f: DocumentFile ->
            f.isFile && f.canRead() && f.isNonEmptyFile() && validSingleFileExtension.matches(f.name!!)
        }

        private val nonLeafDirFilter = { f: DocumentFile ->
            // current f is a readable directory
            // not containing any supported files
            // and contains at least one directory
            f.isDirectory && f.canRead() && f.listFiles()
                .run { !any(audioFileFilter) && any(dirFilter) }
        }

        private val leafDirFilter = { f: DocumentFile ->
            // current f is a readable directory
            // not containing any directory
            // and contains at least one supported file
            f.isDirectory && f.canRead() && f.listFiles()
                .run { !any(dirFilter) && any(audioFileFilter) }
        }
    }

    private val backgroundJob = Job() + CoroutineName(DocumentScanner::class.java.simpleName)
    private val scope = CoroutineScope(Dispatchers.IO + backgroundJob)

    @SuppressLint("UnsafeOptInUsageError")
    private val mediaSourceFactory = DefaultMediaSourceFactory(context) {
        M4bExtractor.createDefaultExtractors(false)
    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    suspend fun scanSources(sourceId: SourceId?): Flow<SourceFile> {
//
//        val activeSources = if (sourceId != null) {
//            listOfNotNull(store.sourceById(sourceId).firstOrNull())
//        } else (store.sources(isActive = true).firstOrNull() ?: emptyList())
//            .sortedBy { -it.createdAt.toEpochMilli() }
//
//        val scanInstant = Instant.now()
//        // var booksAddedCount: Int = 0
//
//        return activeSources.asFlow()
//            .flatMapConcat { source ->
//                scanSource(source, scanInstant)
//                    .onStart {
//                        logcat { "SCAN-COLLECT: sourceId=${source.sourceId} STARTED" }
//                        store.startSourceScan(source.sourceId)
//                    }
//                    .onCompletion { err ->
//                        logcat { "SCAN-COLLECT: sourceId=${source.sourceId} ENDED ${err?.message ?: "WITHOUT ERROR"}" }
//                        store.endSourceScan(source.sourceId)
//                    }
//            }
//    }

    @SuppressLint("UnsafeOptInUsageError")
    suspend fun scanSource(
        source: Source,
        scanInstant: Instant,
        maxLevels: Int = 4,
    ): Flow<SourceFile> = channelFlow {

        val df = DocumentFile.fromTreeUri(context, source.uri)
        logcat { "scanSource: DocumentFile=${df?.uri} canRead=${df?.canRead()}" }
        if (df == null || !df.isDirectory || !df.canRead()) return@channelFlow

//        val sourceBooksByUri = store.bookSourceScansWithBooks(source.sourceId).firstOrNull()
//            ?.associateBy { it.book.uri }
//            ?: emptyMap()

        val channel = Channel<LevelScan>()

        scope.launch {
            levelScan(listOf(df), channel, maxLevels = maxLevels)
            channel.close()
        }

        channel.consumeAsFlow().collect { scan ->
            val pendingFiles = scan.singleAudioFiles + scan.audioFiles

            scan.levelDirs.flatMap { it.listFiles().filter(audioFileFilter) }
                .plus(pendingFiles)
                .mapNotNull { df ->
                    val uri = df.uri
                    logcat { "Processing file: $uri" }

                    try {
                        val mediaItem = MediaItem.Builder().run {
                            setUri(uri)

                            df.name
                                ?.substringAfterLast(".", "")
                                ?.takeIf { ext -> ext.isNotEmpty() }
                                ?.let {
                                    logcat { "extension: $it" }
                                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
                                }
                                ?.let { mimeType ->
                                    logcat { "setMimeType: $mimeType" }
                                    setMimeType(mimeType)
                                }

                            build()
                        }

                        val results = MetadataRetriever.retrieveMetadata(
                            mediaSourceFactory,
                            mediaItem,
                        ).get()

                        val metadata = (0 until results.length).mapNotNull(results::get)
                            .flatMap { trackGroup ->
                                (0 until trackGroup.length).mapNotNull(trackGroup::getFormat)
                            }.fold(M4bMetadata.Builder()) { builder, format ->
                                builder.populateFromFormat(format)
                            }
                            .build()
                            .run {
                                if (durationUs != null && durationUs!! > 0) {
                                    return@run this
                                }

                                // fallback duration
                                val durationMs = MediaMetadataRetriever().run {
                                    setDataSource(context, uri)
                                    val duration = extractMetadata(METADATA_KEY_DURATION)
                                    release()
                                    duration?.toLong()
                                }

                                if (durationMs != null && durationMs > 0) {
                                    buildUpon()
                                        .setDuration(durationMs.milliseconds.inWholeMicroseconds)
                                        .build()
                                } else this
                            }

                        uri to SourceFile(
                            source.sourceId,
                            metadata,
                            metadata.chapters?.map { c -> Triple(uri, c, null) } ?: emptyList(),
                            scanInstant,
                        )
                    } catch (e: Throwable) {
                        logcat { "Failed to process file: ${uri.lastPathSegment}" }
                        e.printStackTrace()
                        null
                    }
                }
                .groupBy { (_, sf) ->
                    val m = sf.metadata

                    listOfNotNull(
                        m.albumArtist ?: m.artist ?: df.parentFile?.name,
                        m.albumTitle ?: m.title ?: df.name,
                        // if (m.albumTitle != m.title) m.title else null,
                    ).joinToString()
                        .trim()
                        .lowercase()
                        .replace(Regex("[^a-z0-9]+"), "")
                }
                .flatMap { (k, pending) ->
                    if (pending.size <= 1) {
                        logcat { "FOUND level book=$k files=${pending.size}" }
                        return@flatMap pending.map { it.second }
                    }

                    val chapterizedFile = pending.filter { (_, it) ->
                        (it.metadata.chapters?.size ?: 0) > 1 && (it.metadata.durationUs ?: 0) > 0
                    }.maxByOrNull { it.second.metadata.chapters?.size ?: 0 }

                    if (chapterizedFile != null) {
                        // if chapterized files exists, use file with most the chapters and ignore all others
                        logcat {
                            "FOUND level book=$k USED CHAPTERIZED chapters=${chapterizedFile.second.chapters.size}"
                        }

                        return@flatMap listOf(chapterizedFile.second)
                    }

                    // prefer using chapterized files (m4b) with at least 1 chapter per file
//                    val (chapterizedFiles, pending) = grp.partition {
//                        it.metadata.chapters?.isNotEmpty() == true
//                    }
//
//                    if (chapterizedFiles.isNotEmpty()) {
//                        // TODO: merge multiple M4bMetadata into a single one
//                        return@flatMap listOf(chapterizedFiles.first())
//                    }

                    if (pending.isNotEmpty()) {
                        // TODO: merge multiple M4bMetadata into a single one
                        val metadata = M4bMetadata.Builder().apply {
                            pending.reversed().forEach { (_, it) ->
                                it.metadata.albumArtist?.let { s -> setAlbumArtist(s) }
                                it.metadata.albumTitle?.let { s -> setAlbumTitle(s) }
                                it.metadata.artist?.let { s -> setArtist(s) }
                                it.metadata.title?.let { s -> setTitle(s) }
                                it.metadata.description?.let { s -> setDescription(s) }
                                it.metadata.compilation?.let { s -> setCompilation(s) }
                                it.metadata.composer?.let { s -> setComposer(s) }
                                it.metadata.artworkData?.let { b ->
                                    setArtworkData(b, it.metadata.artworkDataType)
                                }
                            }
                        }

                        var startTime = 0L
                        var totalDuration = 0L

                        val chapters = pending.fold(mutableListOf<Triple<Uri, ChapterMetadata, M4bMetadata>>()) { acc, (u, o) ->
                            val chapterTitle = o.metadata.title?.toString() ?: "Chapter ${acc.size + 1}"

                            o.metadata.durationUs?.let { duration ->
                                acc.add(Triple(u, ChapterMetadata(startTime, chapterTitle), o.metadata))
                                startTime += duration
                                totalDuration += duration
                            } ?: logcat { "missing duration for $chapterTitle" }
                            acc
                        }.toList()

                        metadata.setDuration(totalDuration)

                        logcat { "merged-pending: totalDuration=$totalDuration chapters=${chapters.size} pending=${pending.size}" }
                        return@flatMap listOf(
                            SourceFile(
                                source.sourceId,
                                metadata.build(),
                                chapters,
                                scanInstant,
                            )
                        )
                    }

                    return@flatMap listOf(
                        SourceFile(
                            source.sourceId,
                            M4bMetadata.Builder().build(),
                            emptyList(),
                            scanInstant,
                        )
                    )
                }.map { send(it) }
        }
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
            levelScan(nextLevelDirs, channel, level + 1, maxLevels, chunkLevelDirSize)
        }
    }

    internal data class LevelScan(
        val level: Int,
        val srcDirs: MutableList<DocumentFile> = mutableListOf(),

        val singleAudioFiles: MutableList<DocumentFile> = mutableListOf(),
        val audioFiles: MutableList<DocumentFile> = mutableListOf(),
        val levelDirs: MutableList<DocumentFile> = mutableListOf(),
    )
}

fun DocumentFile.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)
fun DocumentFile.isImage() = type?.startsWith("image/") == true
fun File.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)
