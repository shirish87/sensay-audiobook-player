package data.entity

import android.net.Uri
import androidx.room.*
import java.time.Instant

@Entity(
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
data class Source(
    @PrimaryKey(autoGenerate = true) val sourceId: Long = 0,
    val uri: Uri,
    val displayName: String,

    val isActive: Boolean = true,
    val inactiveReason: String? = null,

    val createdAt: Instant = Instant.now(),
)

@Entity(
    primaryKeys = ["sourceId", "bookId"],
    indices = [
        Index(value = ["bookId"], unique = true),
    ],
)
data class SourceBookCrossRef(
    val sourceId: Long,
    val bookId: Long,

    val createdAt: Instant = Instant.now(),
)

data class SourceWithBooks(
    @Embedded val source: Source,
    @Relation(
        parentColumn = "sourceId",
        entityColumn = "bookId",
        associateBy = Junction(SourceBookCrossRef::class),
    )
    val books: List<Book>,
)
