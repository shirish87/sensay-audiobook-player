package data.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize
import java.time.Instant

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
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = arrayOf("bookId"),
            childColumns = arrayOf("bookId"),
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Shelf::class,
            parentColumns = arrayOf("shelfId"),
            childColumns = arrayOf("shelfId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
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
