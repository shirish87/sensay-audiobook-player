package data.entity

import androidx.room.*
import data.util.Time

@Entity
data class Tag(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "tagId") val tagId: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sortOrder") val sortOrder: Long = 0,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
)

@Entity(primaryKeys = ["bookId", "tagId"])
data class BookTagCrossRef(
    @ColumnInfo(name = "bookId") val bookId: Long,
    @ColumnInfo(name = "tagId", index = true) val tagId: Long,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
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
