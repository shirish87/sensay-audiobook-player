package config

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.logcat
import javax.inject.Inject

class ConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private const val USER_PREFERENCES = "user_preferences"

        private val KEY_AUDIOBOOKS_FOLDERS = stringSetPreferencesKey("KEY_AUDIOBOOKS_FOLDERS")
        private val KEY_HOME_LAYOUT = stringPreferencesKey("KEY_HOME_LAYOUT")

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

    suspend fun setAudiobookFolders(uris: Set<Uri>) = dataStore.edit { preferences ->
        preferences[KEY_AUDIOBOOKS_FOLDERS] = uris.map { it.toString() }.toSet()
    }

    suspend fun addAudiobookFolders(uris: Set<Uri>) = dataStore.edit { preferences ->
        val value = preferences[KEY_AUDIOBOOKS_FOLDERS] ?: emptySet()
        preferences[KEY_AUDIOBOOKS_FOLDERS] = value + uris.map { it.toString() }.toSet()
    }

    suspend fun clearAudiobookFolders() = dataStore.edit { preferences ->
        preferences[KEY_AUDIOBOOKS_FOLDERS] = emptySet()
    }

    fun getAudiobookFolders(): Flow<Set<Uri>> = dataStore.data.map { preferences ->
        (preferences[KEY_AUDIOBOOKS_FOLDERS] ?: emptySet()).map { it.toUri() }.toSet()
    }

    suspend fun setHomeLayout(layout: HomeLayout) = dataStore.edit { preferences ->
        preferences[KEY_HOME_LAYOUT] = layout.name
    }

    fun getHomeLayout(): Flow<HomeLayout?> = dataStore.data.map { preferences ->
        preferences[KEY_HOME_LAYOUT]?.toString()?.let {
            HomeLayout.valueOf(it)
        }
    }
}
