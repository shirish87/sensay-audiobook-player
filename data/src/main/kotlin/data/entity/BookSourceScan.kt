package data.entity

import android.os.Parcelable
import androidx.room.*
import data.InactiveReason
import data.util.ContentDuration
import data.util.ContentDurationParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = arrayOf("bookId"),
            childColumns = arrayOf("bookId"),
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Source::class,
            parentColumns = arrayOf("sourceId"),
            childColumns = arrayOf("sourceId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
data class BookSourceScan(
    @PrimaryKey(autoGenerate = false) val bookId: BookId,
    val sourceId: SourceId,

    val scanInstant: Instant,
    val isActive: Boolean = true,
    val inactiveReason: InactiveReason? = null,

    val createdAt: Instant = Instant.now(),
) : Parcelable

data class BookSourceScanWithBook(
    @Embedded val bookSourceScan: BookSourceScan,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "bookId",
    )
    val book: Book,
)
