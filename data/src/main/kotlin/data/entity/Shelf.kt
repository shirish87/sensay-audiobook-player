package data.entity

import androidx.room.*
import data.util.Time

@Entity(
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class Shelf(
    @PrimaryKey(autoGenerate = true) val shelfId: Long = 0,
    val name: String,
    val sortOrder: Long = 0,

    val createdAt: Time = Time.now(),
) {
    companion object {
        val ALL = Shelf(shelfId = -1, name = "ALL")
    }
}

@Entity(
    primaryKeys = ["bookId", "shelfId"],
    indices = [
        Index(value = ["shelfId"]),
    ],
)
data class BookShelfCrossRef(
    val bookId: Long,
    val shelfId: Long,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
)

data class ShelfWithBooks(
    @Embedded val shelf: Shelf,
    @Relation(
        parentColumn = "shelfId",
        entityColumn = "bookId",
        associateBy = Junction(BookShelfCrossRef::class),
    )
    val books: List<Book>,
)

data class BooksWithShelves(
    @Embedded val book: Book,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "shelfId",
        associateBy = Junction(BookShelfCrossRef::class),
    )
    val shelves: List<Shelf>,
)
