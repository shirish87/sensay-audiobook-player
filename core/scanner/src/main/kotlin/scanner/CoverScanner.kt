package scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

enum class ImageFormat(val extension: String) {
    webp("webp"),
    jpg("jpg"),
    png("png"),
}

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

        val destFile = newBookCoverFile(context, coverFileId, ImageFormat.webp)
        if (!forceCreate && destFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(context, destFile)

        for (f in documentFile.listFiles().filter { (it.isNonEmptyFile() && it.isImage()) }) {
            val r = runCatching {
                compressCoverFile(context, f, destFile)
            }

            if (r.isFailure) {
                logcat { "scanCoverFromDisk error: ${f.uri} ${r.exceptionOrNull()?.message}" }
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

        val compressedCoverFile = newBookCoverFile(context, coverFileId, ImageFormat.webp)
        if (!forceCreate && compressedCoverFile.isNonEmptyFile())
            return@withContext fileToDocumentFile(context, compressedCoverFile)

        val rawCoverFile = newBookCoverFile(context, coverFileId)
        if (forceCreate || !rawCoverFile.isNonEmptyFile()) {
//            ffmpeg(
//                input = uri,
//                context = context,
//                command = listOf("-an", rawCoverFile.absolutePath),
//            )
        }

        return@withContext if (rawCoverFile.isNonEmptyFile())
            compressCoverFile(
                context,
                fileToDocumentFile(context, rawCoverFile),
                compressedCoverFile,
            )
        else null
    }

    private suspend fun compressCoverFile(
        context: Context,
        srcDocumentFile: DocumentFile?,
        destFile: File,
    ): DocumentFile? = withContext(Dispatchers.IO) {
        if (srcDocumentFile == null) return@withContext null

        val selectedBitmap: Bitmap? =
            context.contentResolver.openInputStream(srcDocumentFile.uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

        selectedBitmap?.let { bitmap ->
            destFile.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, outputStream)
            }

            bitmap.recycle()
        }

        return@withContext fileToDocumentFile(context, destFile)
    }

    private suspend fun newBookCoverFile(
        context: Context,
        coverFileId: String,
        format: ImageFormat = ImageFormat.png,
        coverFile: String = "$coverFileId.${format.extension}",
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
