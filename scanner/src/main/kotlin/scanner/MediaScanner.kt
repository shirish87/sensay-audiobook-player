package scanner

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import javax.inject.Inject

class MediaScanner @Inject constructor(private val mediaAnalyzer: MediaAnalyzer) {

  suspend fun scan(
    folders: List<DocumentFile>,
    audioFileFilter: suspend (fileUri: Uri) -> Boolean,
    onMetadata: suspend (dirUri: Uri?, file: DocumentFile, metadata: Metadata) -> Unit,
  ) {
    val allFiles = folders.flatMap { it.listFiles().toList() }
    allFiles.forEach { analyzeFiles(it, audioFileFilter, onMetadata) }
  }

  private suspend fun analyzeFiles(
    file: DocumentFile,
    audioFileFilter: suspend (fileUri: Uri) -> Boolean,
    onMetadata: suspend (dirUri: Uri?, file: DocumentFile, metadata: Metadata) -> Unit,
  ) {
    if (file.isFile && file.type?.startsWith("audio/") == true) {
      if (audioFileFilter(file.uri)) {
        mediaAnalyzer.analyze(file.uri)?.let {
          onMetadata(file.parentFile?.uri, file, it)
        }
      }
    } else if (file.isDirectory) {
      file.listFiles().forEach {
        analyzeFiles(it, audioFileFilter, onMetadata)
      }
    }
  }
}
