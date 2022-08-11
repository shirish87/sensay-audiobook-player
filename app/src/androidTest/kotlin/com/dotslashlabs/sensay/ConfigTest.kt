package com.dotslashlabs.sensay

import config.ConfigStore
import config.HomeLayout
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ConfigTest : BaseTest() {

    @Inject
    lateinit var configStore: ConfigStore

    @Test
    fun test() = runTest {
        HomeLayout.values().forEach { layout ->
            configStore.setHomeLayout(layout)
            assertEquals(layout, configStore.getHomeLayout().first())
        }
    }
}
