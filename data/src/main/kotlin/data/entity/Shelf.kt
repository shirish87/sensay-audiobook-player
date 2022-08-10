package data.entity

import androidx.room.*
import data.util.Time

@Entity
data class Shelf(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "shelfId") val shelfId: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sortOrder") val sortOrder: Long = 0,

    @ColumnInfo(name = "createdAt") val createdAt: Time = Time.now(),
) {
    companion object {
        val ALL = Shelf(shelfId = -1, name = "ALL")
    }
}

@Entity(primaryKeys = ["bookId", "shelfId"])
data class BookShelfCrossRef(
    @ColumnInfo(name = "bookId") val bookId: Long,
    @ColumnInfo(name = "shelfId", index = true) val shelfId: Long,

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
