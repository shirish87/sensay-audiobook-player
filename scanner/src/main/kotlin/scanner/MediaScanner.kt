package scanner

import androidx.documentfile.provider.DocumentFile
import javax.inject.Inject

class MediaScanner @Inject constructor(private val mediaAnalyzer: MediaAnalyzer) {

    private val supportedExtensions = listOf("m4a", "m4b", "mp4", "mp3", "mp2", "mp1", "ogg", "wav")

    private val validAudiobookFileExtension by lazy {
        ".*\\.(${supportedExtensions.joinToString("|")})$".toRegex(
            RegexOption.IGNORE_CASE
        )
    }

    suspend fun scan(
        folders: List<DocumentFile>,
        audioFileFilter: suspend (file: DocumentFile) -> Boolean,
        onMetadata: suspend (file: DocumentFile, metadata: Metadata) -> Unit,
    ) {
        val allFiles = folders
            .filter { it.isDirectory && it.canRead() }
            .flatMap { it.listFiles().toList() }

        allFiles.forEach { analyzeFiles(it, audioFileFilter, onMetadata) }
    }

    private suspend fun analyzeFiles(
        file: DocumentFile,
        audioFileFilter: suspend (file: DocumentFile) -> Boolean,
        onMetadata: suspend (file: DocumentFile, metadata: Metadata) -> Unit,
    ) {
        if (file.isFile) {
            if (file.type?.startsWith("audio/", true) != true &&
                !validAudiobookFileExtension.matches(file.uri.toString())
            ) return

            if (!audioFileFilter(file)) return

            mediaAnalyzer.analyze(file.uri)?.let {
                onMetadata(file, it)
            }
        } else if (file.isDirectory) {
            file.listFiles().forEach {
                analyzeFiles(it, audioFileFilter, onMetadata)
            }
        }
    }
}
