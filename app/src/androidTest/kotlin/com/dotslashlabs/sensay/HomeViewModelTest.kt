package com.dotslashlabs.sensay

import androidx.core.net.toUri
import com.dotslashlabs.sensay.ui.screen.home.HomeViewModel
import com.dotslashlabs.sensay.ui.screen.home.HomeViewState
import config.ConfigStore
import dagger.hilt.android.testing.HiltAndroidTest
import data.SensayStore
import data.entity.Book
import data.entity.BookWithChapters
import data.entity.Chapter
import data.util.Time
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltAndroidTest
class HomeViewModelTest : BaseTest() {

    @Inject
    lateinit var sensayStore: SensayStore

    @Inject
    lateinit var configStore: ConfigStore

    @Test
    fun testHomeViewModel() = runTest {
        val viewModel = HomeViewModel(
            HomeViewState(),
            sensayStore,
            configStore,
        )

        val booksWithChapters = (1..10).map { bookId ->
            BookWithChapters(
                book = Book(
                    uri = "test://$bookId".toUri(),
                    duration = Time(value = 1.hours),
                    title = "Hungry Potter $bookId",
                    author = "J. K. Growling",
                    hash = "$bookId",
                ),
                chapters = (1..10).map { c ->
                    Chapter(
                        trackId = c,
                        title = "Chapter $c",
                        duration = Time(value = 1.hours),
                        uri = "$c".toUri(),
                        hash = "$c",
                    )
                }
            )
        }

        viewModel.createBooksWithChapters(booksWithChapters)
        assertEquals(booksWithChapters.size, viewModel.booksCount().first())
    }
}
