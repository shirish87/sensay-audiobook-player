package data.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import data.InactiveReason
import data.util.ContentDuration
import data.util.ContentDurationParceler
import kotlinx.parcelize.IgnoredOnParcel
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
    indices = [
        Index(value = ["sourceId"]),
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
    val isRemote: Boolean = false,

    val createdAt: Instant = Instant.now(),
) : Parcelable

@Parcelize
data class BookSourceScanWithBook(
    @Embedded val bookSourceScan: BookSourceScan,

    @Relation(
        parentColumn = "bookId",
        entityColumn = "bookId",
    )
    val book: Book,

    @Relation(
        parentColumn = "bookId",
        entityColumn = "bookId",
    )
    val bookProgress: BookProgress,

    @Relation(
        parentColumn = "bookId",
        entityColumn = "bookId",
    )
    val chapters: List<Chapter>,
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    val chapter: Chapter? = chapters.firstOrNull { it.chapterId == bookProgress.chapterId }
}
