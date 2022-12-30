package data.dao

import androidx.room.Dao
import androidx.room.Query
import data.entity.BookConfig
import data.entity.BookId
import kotlinx.coroutines.flow.Flow

@Dao
interface BookConfigDao : BaseDao<BookConfig> {

    @Query("SELECT * FROM BookConfig WHERE bookId = :bookId")
    fun bookConfig(bookId: BookId): Flow<BookConfig>
}
