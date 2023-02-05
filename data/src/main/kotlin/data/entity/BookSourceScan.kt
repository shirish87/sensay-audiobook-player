package data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import data.InactiveReason
import kotlinx.parcelize.Parcelize
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
data class BookSourceScan(
    @PrimaryKey(autoGenerate = false) val bookId: BookId,
    val sourceId: SourceId,

    val scanInstant: Instant,
    val isActive: Boolean = true,
    val inactiveReason: InactiveReason? = null,

    val createdAt: Instant = Instant.now(),
) : Parcelable
