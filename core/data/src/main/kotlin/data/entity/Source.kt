package data.entity

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant

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

    val isScanning: Boolean = false,

    val createdAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = Source(
            uri = Uri.EMPTY,
            displayName = "",
        )
    }
}
