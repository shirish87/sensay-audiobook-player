package com.dotslashlabs.sensay

import androidx.core.net.toUri
import dagger.hilt.android.testing.HiltAndroidTest
import data.SensayStore
import data.entity.Book
import data.entity.BookWithChapters
import data.entity.Chapter
import data.util.ContentDuration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltAndroidTest
class SensayStoreTest : BaseTest() {

    @Inject
    lateinit var sensayStore: SensayStore

    @Test
    fun testCreateBooksWithChapters() = runTest {
        val booksWithChapters = (1..10).map { bookId ->
            BookWithChapters(
                book = Book(
                    uri = "test://$bookId".toUri(),
                    duration = ContentDuration(value = 1.hours),
                    title = "Hungry Potter $bookId",
                    author = "J. K. Growling",
                    hash = "$bookId",
                ),
                chapters = (1..10).map { c ->
                    Chapter(
                        trackId = c,
                        title = "Chapter $c",
                        duration = ContentDuration(value = 1.hours),
                        uri = "$c".toUri(),
                        hash = "$c",
                    )
                }
            )
        }

        sensayStore.createBooksWithChapters(1L, booksWithChapters)
        assertEquals(booksWithChapters.size, sensayStore.booksCount().first())
    }
}
