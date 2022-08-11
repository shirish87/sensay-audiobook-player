package data.repository

import android.net.Uri
import data.dao.SourceDao
import data.entity.Source
import data.entity.SourceBookCrossRef
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SourceRepository @Inject constructor(
    private val sourceDao: SourceDao,
) {

    fun sources() = sourceDao.sources()

    fun sources(isActive: Boolean = true) = sourceDao.sources(isActive)

    fun sourceWithBooks(sourceId: Long) = sourceDao.sourceWithBooks(sourceId)

    fun sourceByUri(uri: Uri) = sourceDao.sourceByUri(uri)

    fun sourcesCount() = sourceDao.sourcesCount()

    fun sourcesMaxCreatedAtTime() = sourceDao.sourcesMaxCreatedAtTime()

    suspend fun addSources(sources: Collection<Source>): Int {
        val existingSourceUris = (sourceDao.sources().firstOrNull() ?: emptyList())
            .map { it.uri }
            .toSet()

        val newSources = sources.filterNot { existingSourceUris.contains(it.uri) }
        if (newSources.isEmpty()) return 0

        return sourceDao.insertAll(sources).size
    }

    suspend fun insertSourceBookCrossRef(ref: SourceBookCrossRef) =
        sourceDao.insertSourceBookCrossRef(ref)

    suspend fun deleteSource(source: Source) {
        sourceDao.sourceBookCrossRefs(source.sourceId).firstOrNull()?.let {
            sourceDao.deleteSourceBookCrossRefs(it)
        }

        sourceDao.delete(source)
    }
}
