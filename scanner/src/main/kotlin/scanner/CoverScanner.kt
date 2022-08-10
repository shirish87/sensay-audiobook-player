package scanner

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat
import java.io.File
import javax.inject.Inject

fun DocumentFile.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)
fun DocumentFile.isImage() = type?.startsWith("image/") == true
fun File.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)

class CoverScanner @Inject constructor(private val context: Context) {

    suspend fun scanCover(
        folderUri: Uri?,
        uri: Uri,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = folderUri?.let { bookFolderUri ->
        scanCoverFromDisk(bookFolderUri, coverFileId, forceCreate)
    } ?: scanForEmbeddedCover(uri, coverFileId, forceCreate)

    suspend fun scanCoverFromDisk(
        folderUri: Uri,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = withContext(Dispatchers.IO) {
        logcat { "scanCoverFromDisk: $folderUri" }

        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext null
        if (!documentFile.isDirectory) return@withContext null

        val destFile = newBookCoverFile(coverFileId)
        if (!forceCreate && destFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(destFile)

        for (f in documentFile.listFiles().filter { (it.isNonEmptyFile() && it.isImage()) }) {
            runCatching {
                context.contentResolver.openInputStream(f.uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }

                if (destFile.isNonEmptyFile()) {
                    return@withContext fileToDocumentFile(destFile)
                }
            }
        }

        return@withContext null
    }

    suspend fun scanForEmbeddedCover(
        uri: Uri,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = withContext(Dispatchers.IO) {
        logcat { "scanForEmbeddedCover: $uri" }

        val coverFile = newBookCoverFile(coverFileId)

        if (!forceCreate && coverFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(coverFile)

        ffmpeg(
            input = uri,
            context = context,
            command = listOf("-an", coverFile.absolutePath),
        )

        return@withContext if (coverFile.isNonEmptyFile())
            fileToDocumentFile(coverFile)
        else null
    }

    private suspend fun newBookCoverFile(
        coverFileId: String,
        coverFile: String = "${coverFileId}.png",
    ): File = withContext(Dispatchers.IO) {
        val coversFolder = File(context.cacheDir, "covers")
        if (!coversFolder.exists()) {
            coversFolder.mkdirs()
        }

        File(coversFolder, coverFile)
    }

    private fun fileToDocumentFile(file: File) = DocumentFile.fromSingleUri(
        context,
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file),
    )
}
