package com.dotslashlabs.sensay

import com.airbnb.mvrx.test.MvRxTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Before
import org.junit.Rule
import org.junit.runners.model.TestTimedOutException
import java.util.concurrent.TimeUnit

abstract class BaseTest {

    @get:Rule(order = 0)
    val hiltRule by lazy { HiltAndroidRule(this) }

    @get:Rule(order = 1)
    val mvrxRule = MvRxTestRule()

    @Before
    fun init() {
        hiltRule.inject()
    }

    suspend fun <T> runWithTimeout(timeMillis: Long, block: suspend CoroutineScope.() -> T): T? {
        return withTimeoutOrNull(timeMillis, block)
            ?: throw TestTimedOutException(timeMillis, TimeUnit.MILLISECONDS)
    }
}
