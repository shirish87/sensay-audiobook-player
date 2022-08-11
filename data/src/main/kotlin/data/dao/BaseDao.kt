package data.dao

import androidx.room.*

interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: T): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: Collection<T>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: T): Int

    @Delete
    suspend fun delete(entity: T): Int

    @Transaction
    @Delete
    suspend fun deleteAll(entities: Collection<T>): Int
}

@Transaction
suspend inline fun <reified T> BaseDao<T>.insertOrUpdate(item: T): Long? {
    val rowId = insert(item)
    if (rowId != -1L) {
        return rowId
    }

    update(item)
    return null
}
