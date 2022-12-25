package data.entity

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import java.time.Instant

typealias TagId = Long

@Entity(
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
@Parcelize
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: TagId = 0,
    val name: String,
    val sortOrder: Long = 0,

    val createdAt: Instant = Instant.now(),
) : Parcelable

@Entity(
    primaryKeys = ["bookId", "tagId"],
    indices = [
        Index(value = ["tagId"]),
    ],
)
data class BookTagCrossRef(
    val bookId: BookId,
    val tagId: TagId,

    val createdAt: Instant = Instant.now(),
)

data class TagWithBooks(
    @Embedded val tag: Tag,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "bookId",
        associateBy = Junction(BookTagCrossRef::class),
    )
    val books: List<Book>,
)

data class BookWithTags(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "tagId",
        associateBy = Junction(BookTagCrossRef::class),
    )
    val tags: List<Tag>,
)
