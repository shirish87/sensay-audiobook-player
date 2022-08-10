package data.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import data.util.Time

@Entity
data class Book(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "bookId") val bookId: Long = 0,
    @ColumnInfo(name = "hash", index = true) val hash: String,
    @ColumnInfo(name = "uri", index = true) val uri: Uri,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "duration") val duration: Time,

    @ColumnInfo(name = "series") val series: String? = null,
    @ColumnInfo(name = "bookNo") val bookNo: Float? = null,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "author") val author: String? = null,
    @ColumnInfo(name = "narrator") val narrator: String? = null,
    @ColumnInfo(name = "year") val year: String? = null,
    @ColumnInfo(name = "coverUri") val coverUri: Uri? = null,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
)
