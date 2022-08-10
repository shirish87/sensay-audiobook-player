package com.dotslashlabs.sensay

import android.net.Uri
import config.ConfigStore
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ConfigTest : BaseTest() {

    @Inject
    lateinit var configStore: ConfigStore

    @Test
    @Throws(Exception::class)
    fun setAudiobooksHome() = runBlocking {
        val uri = Uri.parse("/mnt/sdcard/audiobooks")

        configStore.setAudiobooksHome(uri)
        assertEquals(uri, configStore.getAudiobooksHome().first())
    }
}
