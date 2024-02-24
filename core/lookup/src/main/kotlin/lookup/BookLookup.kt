package lookup

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.logcat
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import javax.inject.Inject


class BookLookup @Inject constructor(
    private val client: OkHttpClient,
) {

    companion object {
        private const val G_BOOKS_API: String = "https://www.googleapis.com/books/v1/volumes"
    }

    private val moshi: Moshi = Moshi.Builder().build()
    private val volumesJsonAdapter: JsonAdapter<Volumes>? = moshi.adapter(Volumes::class.java)

    suspend fun lookup(
        bookTitle: String,
        author: String?,
        series: String?,
    ): List<Item> = withContext(Dispatchers.IO) {
        val adapter = volumesJsonAdapter ?: return@withContext emptyList()

        val httpBuilder: HttpUrl.Builder = G_BOOKS_API.toHttpUrlOrNull()!!.newBuilder()
        val query = listOfNotNull(
            series,
            "intitle:$bookTitle",
            author?.run { "inauthor:$this" },
        ).joinToString("+")

        httpBuilder.addQueryParameter("q", query)

        val request: Request = Request.Builder()
            .url(httpBuilder.build())
            .get()
            .build()

        return@withContext client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            logcat { "Response: success $query" }
            val r = response.body?.source()?.let(adapter::fromJson)
            logcat { "Response: $r" }
            if (r == null || r.items.isEmpty()) return@use emptyList()

            return@use r.items
        }
    }
}

@JsonClass(generateAdapter = true)
data class Volumes(var items: List<Item>)

@JsonClass(generateAdapter = true)
data class Item(
    var kind: String? = null,
    var id: String? = null,
    var etag: String? = null,
    var selfLink: String? = null,
    var volumeInfo: VolumeInfo? = VolumeInfo(),
    var searchInfo: SearchInfo? = SearchInfo()
)

@JsonClass(generateAdapter = true)
data class IndustryIdentifiers(
    var type: String? = null,
    var identifier: String? = null
)

@JsonClass(generateAdapter = true)
data class VolumeInfo(
    var title: String? = null,
    var subtitle: String? = null,
    var authors: List<String> = listOf(),
    var publisher: String? = null,
    var publishedDate: String? = null,
    var description: String? = null,
    var industryIdentifiers: List<IndustryIdentifiers> = listOf(),
    var pageCount: Int? = null,
    var printType: String? = null,
    var categories: List<String> = listOf(),
    var averageRating: Double? = null,
    var ratingsCount: Int? = null,
    var maturityRating: String? = null,
    var allowAnonLogging: Boolean? = null,
    var contentVersion: String? = null,
    var imageLinks: ImageLinks? = ImageLinks(),
    var language: String? = null,
    var previewLink: String? = null,
    var infoLink: String? = null,
    var canonicalVolumeLink: String? = null
)

@JsonClass(generateAdapter = true)
data class ImageLinks(
    var smallThumbnail: String? = null,
    var thumbnail: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchInfo(
    var textSnippet: String? = null
)
