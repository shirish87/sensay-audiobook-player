package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Entity(
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
@Parcelize
data class Source(
    @PrimaryKey(autoGenerate = true) val sourceId: Long = 0,
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
