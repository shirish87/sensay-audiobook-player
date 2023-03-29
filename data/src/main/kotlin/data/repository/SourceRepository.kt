package data.repository

import android.net.Uri
import data.InactiveReason
import data.dao.SourceDao
import data.dao.SourceScanUpdate
import data.entity.BookId
import data.entity.BookSourceScan
import data.entity.Source
import data.entity.SourceId
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SourceRepository @Inject constructor(
    private val sourceDao: SourceDao,
) {

    fun sourceById(sourceId: SourceId) = sourceDao.sourceById(sourceId)

    fun sources() = sourceDao.sources()

    fun sources(isActive: Boolean = true) = sourceDao.sources(isActive)

    fun sourceByUri(uri: Uri) = sourceDao.sourceByUri(uri)

    fun sourcesCount() = sourceDao.sourcesCount()

    fun sourcesMaxCreatedAtTime() = sourceDao.sourcesMaxCreatedAtTime()

    suspend fun updateSource(
        sourceId: SourceId,
        isScanning: Boolean,
    ): Int = sourceDao.updateSource(
        SourceScanUpdate(
            sourceId,
            isScanning,
        ),
    )

    suspend fun updateSourceBooks(
        sourceId: SourceId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
    ): Int = sourceDao.updateSourceBooks(
        sourceId, isActive, inactiveReason,
    )

    suspend fun updateSourceBook(
        sourceId: SourceId,
        bookId: BookId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
    ): Int = sourceDao.updateBookSource(
        bookId, sourceId, isActive, inactiveReason,
    )

    suspend fun upsertBookSourceScan(bookSourceScan: BookSourceScan) =
        sourceDao.upsertBookSourceScan(bookSourceScan)

    fun sourceBooks(sourceId: SourceId) = sourceDao.sourceBooks(sourceId)

    fun bookSourceScansWithBooks(sourceId: SourceId) = sourceDao.bookSourceScansWithBooks(sourceId)

    fun sourceBooks(sourceId: SourceId, isActive: Boolean) =
        sourceDao.sourceBooks(sourceId, isActive)

    suspend fun addSources(sources: Collection<Source>): Int {
        val existingSourceUris = (sourceDao.sources().firstOrNull() ?: emptyList())
            .map { it.uri }
            .toSet()

        val newSources = sources.filterNot { existingSourceUris.contains(it.uri) }
        if (newSources.isEmpty()) return 0

        return sourceDao.insertAll(sources).size
    }

    suspend fun deleteSource(sourceId: SourceId) {
        sourceDao.delete(Source.empty().copy(sourceId = sourceId))
    }
}
