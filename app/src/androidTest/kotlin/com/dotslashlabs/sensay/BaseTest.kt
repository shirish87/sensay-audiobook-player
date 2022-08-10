package com.dotslashlabs.sensay

import com.airbnb.mvrx.test.MvRxTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.TestTimedOutException
import java.util.concurrent.TimeUnit

// Reusable JUnit4 TestRule to override the Main dispatcher
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

abstract class BaseTest {

    @get:Rule(order = 0)
    val hiltRule by lazy { HiltAndroidRule(this) }

    @get:Rule(order = 1)
    val mvrxRule = MvRxTestRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    suspend fun <T> runWithTimeout(timeMillis: Long, block: suspend CoroutineScope.() -> T): T? {
        return withTimeoutOrNull(timeMillis, block)
            ?: throw TestTimedOutException(timeMillis, TimeUnit.MILLISECONDS)
    }
}
