package data.entity

import android.os.Parcelable
import androidx.room.*
import java.time.Instant
import kotlinx.parcelize.Parcelize

typealias ShelfId = Long

@Entity(
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
@Parcelize
data class Shelf(
    @PrimaryKey(autoGenerate = true) val shelfId: ShelfId = 0,
    val name: String,
    val sortOrder: Long = 0,

    val createdAt: Instant = Instant.now(),
) : Parcelable {
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
    val bookId: BookId,
    val shelfId: ShelfId,

    val createdAt: Instant = Instant.now(),
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
