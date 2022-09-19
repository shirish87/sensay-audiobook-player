package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.*
import java.time.Instant
import kotlinx.parcelize.Parcelize

typealias SourceId = Long

@Entity(
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
@Parcelize
data class Source(
    @PrimaryKey(autoGenerate = true) val sourceId: SourceId = 0,
    val uri: Uri,
    val displayName: String,

    val isActive: Boolean = true,
    val inactiveReason: String? = null,

    val createdAt: Instant = Instant.now(),
) : Parcelable

@Entity(
    primaryKeys = ["sourceId", "bookId"],
    indices = [
        Index(value = ["bookId"], unique = true),
    ],
)
data class SourceBookCrossRef(
    val sourceId: SourceId,
    val bookId: BookId,

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
