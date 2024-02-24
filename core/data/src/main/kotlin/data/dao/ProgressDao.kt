package data.dao

import androidx.room.Dao
import androidx.room.Query
import data.entity.Progress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao : BaseDao<Progress> {

    @Query("SELECT COUNT(*) FROM Progress")
    fun progressCount(): Flow<Int>

    @Query("SELECT * FROM Progress")
    fun progressRestorable(): Flow<List<Progress>>
}
