package data.dao

import android.net.Uri
import androidx.room.*
import data.entity.Source
import data.entity.SourceBookCrossRef
import data.entity.SourceWithBooks
import data.util.ContentDuration
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao : BaseDao<Source> {
    @Query("SELECT * FROM Source")
    fun sources(): Flow<List<Source>>

    @Query("SELECT * FROM Source WHERE isActive = :isActive")
    fun sources(isActive: Boolean = true): Flow<List<Source>>

    @Query("SELECT * FROM Source WHERE sourceId = :sourceId")
    fun sourceById(sourceId: Long): Flow<Source>

    @Query("SELECT * FROM Source WHERE uri = :uri")
    fun sourceByUri(uri: Uri): Flow<Source>

    @Query("SELECT COUNT(*) FROM Source")
    fun sourcesCount(): Flow<Int>

    @Query("SELECT MAX(createdAt) FROM Source")
    fun sourcesMaxCreatedAtTime(): Flow<ContentDuration?>

    @Transaction
    @Query("SELECT * FROM Source WHERE sourceId = :sourceId")
    fun sourceWithBooks(sourceId: Long): Flow<SourceWithBooks>

    @Query("SELECT * FROM SourceBookCrossRef WHERE sourceId = :sourceId")
    fun sourceBookCrossRefs(sourceId: Long): Flow<List<SourceBookCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSourceBookCrossRef(ref: SourceBookCrossRef): Long

    @Delete
    suspend fun deleteSourceBookCrossRefs(refs: Collection<SourceBookCrossRef>): Int
}
