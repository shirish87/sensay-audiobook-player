package data.entity

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import data.util.ContentDuration
import java.time.Instant

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
    val duration: ContentDuration,

    val series: String? = null,
    val bookNo: Float? = null,
    val description: String? = null,
    val author: String? = null,
    val narrator: String? = null,
    val year: String? = null,
    val coverUri: Uri? = null,

    val createdAt: Instant = Instant.now(),
) {

    companion object {
        fun empty() = Book(
            hash = "",
            uri = Uri.EMPTY,
            title = "",
            duration = ContentDuration.ZERO,
        )

        fun fromBundle(bundle: Bundle) = Book(
            bookId = bundle.getLong("bookId"),
            hash = bundle.getString("hash", null),
            uri = bundle.getString("uri", null)?.let { Uri.parse(it) } ?: Uri.EMPTY,
            title = bundle.getString("title", null),
            duration = ContentDuration.parse(bundle.getString("duration", null)),
            series = bundle.getString("series", null),
            bookNo = bundle.getFloat("bookNo"),
            description = bundle.getString("description", null),
            author = bundle.getString("author", null),
            narrator = bundle.getString("narrator", null),
            year = bundle.getString("year", null),
            coverUri = bundle.getString("coverUri", null)?.let { Uri.parse(it) } ?: Uri.EMPTY,
            createdAt = Instant.ofEpochMilli(bundle.getLong("createdAt")),
        )
    }

    fun toBundle() = bundleOf(
        "bookId" to bookId,
        "hash" to hash,
        "uri" to uri.toString(),
        "title" to title,
        "duration" to duration.format(),
        "series" to series,
        "bookNo" to bookNo,
        "description" to description,
        "author" to author,
        "narrator" to narrator,
        "year" to year,
        "coverUri" to coverUri.toString(),
        "createdAt" to createdAt.toEpochMilli(),
    )
}
