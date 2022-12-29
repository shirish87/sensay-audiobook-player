package com.dotslashlabs.sensay

import dagger.hilt.android.testing.HiltAndroidTest
import data.SensayStore
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SensayStoreTest : BaseTest() {

    @Inject
    lateinit var sensayStore: SensayStore

    @Test
    fun testCreateBooksWithChapters() = runTest {
//        val booksWithChapters = (1..10).map { bookId ->
//            BookWithChapters(
//                book = Book(
//                    uri = "test://$bookId".toUri(),
//                    duration = ContentDuration(value = 1.hours),
//                    title = "Hungry Potter $bookId",
//                    author = "J. K. Growling",
//                    hash = "$bookId",
//                ),
//                chapters = (1..10).map { c ->
//                    Chapter(
//                        trackId = c,
//                        title = "Chapter $c",
//                        duration = ContentDuration(value = 1.hours),
//                        uri = "$c".toUri(),
//                        hash = "$c",
//                        start = ContentDuration.ZERO,
//                        end = ContentDuration(value = 1.hours),
//                    )
//                }
//            )
//        }
//
//        sensayStore.createOrUpdateBooksWithChapters(1L, booksWithChapters)
//        assertEquals(booksWithChapters.size, sensayStore.booksCount().first())
    }
}
