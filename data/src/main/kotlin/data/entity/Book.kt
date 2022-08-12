package data.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import data.util.Time

@Entity(
    indices = [
        Index(value = ["hash"], unique = true),
        Index(value = ["uri"], unique = true),
    ],
)
data class Book(
    @PrimaryKey(autoGenerate = true) val bookId: Long = 0,
    val hash: String,
    val uri: Uri,
    val title: String,
    val duration: Time,

    val series: String? = null,
    val bookNo: Float? = null,
    val description: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val year: String? = null,
    val coverUri: Uri? = null,

    val createdAt: Time = Time.now(),
) {

    companion object {
        fun empty() = Book(
            hash = "",
            uri = Uri.EMPTY,
            title = "",
            duration = Time.zero(),
        )
    }
}
