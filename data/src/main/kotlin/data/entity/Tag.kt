package data.entity

import androidx.room.*
import data.util.Time

@Entity(
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0,
    val name: String,
    val sortOrder: Long = 0,

    val createdAt: Time = Time.now(),
)

@Entity(
    primaryKeys = ["bookId", "tagId"],
    indices = [
        Index(value = ["tagId"]),
    ],
)
data class BookTagCrossRef(
    val bookId: Long,
    val tagId: Long,

    val createdAt: Time = Time.now(),
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
