package config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private const val USER_PREFERENCES = "user_preferences"

        private val KEY_HOME_LAYOUT = stringPreferencesKey("KEY_HOME_LAYOUT")

        private val KEY_AUDIOBOOKS_FOLDERS_LAST_UPDATE =
            longPreferencesKey("KEY_AUDIOBOOKS_FOLDERS_LAST_UPDATE")

        private val KEY_LAST_PLAYED_BOOK_ID = longPreferencesKey("KEY_LAST_PLAYED_BOOK_ID")

        fun instance(appContext: Context) = ConfigStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = { appContext.preferencesDataStoreFile(USER_PREFERENCES) },
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() },
                ),
                migrations = listOf(
                    SharedPreferencesMigration(appContext, USER_PREFERENCES),
                ),
            ),
        )
    }

    suspend fun setHomeLayout(layout: HomeLayout) = dataStore.edit { preferences ->
        preferences[KEY_HOME_LAYOUT] = layout.name
    }

    fun getHomeLayout(): Flow<HomeLayout?> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_LAYOUT]?.toString()?.let {
            HomeLayout.valueOf(it)
        }
    }

    suspend fun setAudiobookFoldersUpdateTime(instant: Instant) = dataStore.edit { preferences ->
        preferences[KEY_AUDIOBOOKS_FOLDERS_LAST_UPDATE] = instant.toEpochMilli()
    }

    fun getAudiobookFoldersUpdateTime(): Flow<Instant> = dataStore.data.map { preferences ->
        Instant.ofEpochMilli(preferences[KEY_AUDIOBOOKS_FOLDERS_LAST_UPDATE] ?: 0L)
    }

    suspend fun setLastPlayedBookId(bookId: Long?) = dataStore.edit { preferences ->
        preferences[KEY_LAST_PLAYED_BOOK_ID] = bookId ?: -1L
    }

    fun getLastPlayedBookId(): Flow<Long?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_PLAYED_BOOK_ID]
    }
}
