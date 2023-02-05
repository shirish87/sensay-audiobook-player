package data.dao

import android.net.Uri
import androidx.room.*
import data.InactiveReason
import data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SourceDao : BaseDao<Source> {
    @Query("SELECT * FROM Source")
    fun sources(): Flow<List<Source>>

    @Query("SELECT * FROM Source WHERE isActive = :isActive")
    fun sources(isActive: Boolean = true): Flow<List<Source>>

    @Query("SELECT * FROM Source WHERE sourceId = :sourceId")
    fun sourceById(sourceId: SourceId): Flow<Source>

    @Query("SELECT * FROM Source WHERE uri = :uri")
    fun sourceByUri(uri: Uri): Flow<Source>

    @Query("SELECT COUNT(*) FROM Source")
    fun sourcesCount(): Flow<Int>

    @Query("SELECT MAX(createdAt) FROM Source")
    fun sourcesMaxCreatedAtTime(): Flow<Instant?>

    @Update(entity = Source::class)
    suspend fun updateSource(vararg sourceScanUpdate: SourceScanUpdate): Int

    @Query("UPDATE BookSourceScan SET isActive = :isActive, inactiveReason = :inactiveReason WHERE sourceId = :sourceId")
    suspend fun updateSourceBooks(
        sourceId: SourceId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
    ): Int

    @Query("UPDATE BookSourceScan SET isActive = :isActive, inactiveReason = :inactiveReason WHERE bookId = :bookId AND sourceId = :sourceId")
    suspend fun updateBookSource(
        bookId: BookId,
        sourceId: SourceId,
        isActive: Boolean,
        inactiveReason: InactiveReason? = null,
    ): Int

    @Upsert(entity = BookSourceScan::class)
    suspend fun upsertBookSourceScan(vararg bookSourceScan: BookSourceScan)

    @Query("SELECT * FROM BookSourceScan WHERE sourceId = :sourceId")
    fun sourceBooks(sourceId: SourceId): Flow<List<BookSourceScan>>

    @Query("SELECT * FROM BookSourceScan WHERE sourceId = :sourceId AND isActive = :isActive")
    fun sourceBooks(sourceId: SourceId, isActive: Boolean): Flow<List<BookSourceScan>>
}

data class SourceScanUpdate(
    val sourceId: SourceId,
    val isScanning: Boolean,
)
