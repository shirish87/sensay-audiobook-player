package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import data.InactiveReason
import data.util.ContentDuration
import data.util.ContentDurationParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

typealias BookId = Long

@Entity(
    indices = [
        Index(value = ["hash"], unique = true),
        Index(value = ["uri"], unique = true),
    ],
)
@Parcelize
@TypeParceler<ContentDuration, ContentDurationParceler>()
data class Book(
    @PrimaryKey(autoGenerate = true) val bookId: BookId = 0,
    val hash: String,
    val uri: Uri,
    val title: String,
    val duration: ContentDuration,

    val series: String? = null,
    val bookNo: Float? = null,
    val description: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val year: String? = null,
    val coverUri: Uri? = null,
    val lastModified: Instant? = null,

    val isActive: Boolean = true,
    val inactiveReason: InactiveReason? = null,
    val scanInstant: Instant? = null,

    val createdAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = Book(
            hash = "",
            uri = Uri.EMPTY,
            title = "",
            duration = ContentDuration.ZERO,
        )
    }
}
