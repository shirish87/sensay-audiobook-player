package scanner

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat

fun DocumentFile.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)
fun DocumentFile.isImage() = type?.startsWith("image/") == true
fun File.isNonEmptyFile() = (isFile && exists() && canRead() && length() > 0)

class CoverScanner @Inject constructor() {

    suspend fun scanCover(
        context: Context,
        file: DocumentFile,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = file.parentFile?.uri?.let { bookFolderUri ->
        scanCoverFromDisk(context, bookFolderUri, coverFileId, forceCreate)
    } ?: scanForEmbeddedCover(context, file.uri, coverFileId, forceCreate)

    suspend fun scanCoverFromDisk(
        context: Context,
        folderUri: Uri,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = withContext(Dispatchers.IO) {
        logcat { "scanCoverFromDisk: $folderUri" }

        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: return@withContext null
        if (!documentFile.isDirectory) return@withContext null

        val destFile = newBookCoverFile(context, coverFileId)
        if (!forceCreate && destFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(context, destFile)

        for (f in documentFile.listFiles().filter { (it.isNonEmptyFile() && it.isImage()) }) {
            runCatching {
                context.contentResolver.openInputStream(f.uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }

                if (destFile.isNonEmptyFile()) {
                    return@withContext fileToDocumentFile(context, destFile)
                }
            }
        }

        return@withContext null
    }

    suspend fun scanForEmbeddedCover(
        context: Context,
        uri: Uri,
        coverFileId: String,
        forceCreate: Boolean = false,
    ): DocumentFile? = withContext(Dispatchers.IO) {
        logcat { "scanForEmbeddedCover: $uri" }

        val coverFile = newBookCoverFile(context, coverFileId)

        if (!forceCreate && coverFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(context, coverFile)

        ffmpeg(
            input = uri,
            context = context,
            command = listOf("-an", coverFile.absolutePath),
        )

        return@withContext if (coverFile.isNonEmptyFile())
            fileToDocumentFile(context, coverFile)
        else null
    }

    private suspend fun newBookCoverFile(
        context: Context,
        coverFileId: String,
        coverFile: String = "$coverFileId.png",
    ): File = withContext(Dispatchers.IO) {
        val coversFolder = File(context.cacheDir, "covers")
        if (!coversFolder.exists()) {
            coversFolder.mkdirs()
        }

        File(coversFolder, coverFile)
    }

    private fun fileToDocumentFile(context: Context, file: File) = DocumentFile.fromSingleUri(
        context,
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file),
    )
}
