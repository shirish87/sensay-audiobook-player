package data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = arrayOf("bookId"),
            childColumns = arrayOf("bookId"),
            onDelete = CASCADE,
        ),
    ],
)
@Parcelize
data class BookConfig(
    @PrimaryKey(autoGenerate = false) val bookId: BookId = 0,
    val isVolumeBoostEnabled: Boolean = false,
    val isBassBoostEnabled: Boolean = false,
    val isReverbEnabled: Boolean = false,
    val isSkipSilenceEnabled: Boolean = false,
    val lastModified: Instant? = null,
    val createdAt: Instant = Instant.now(),
) : Parcelable {

    companion object {
        fun empty() = BookConfig(
            bookId = 0L,
        )
    }
}
