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
import javax.inject.Inject

class ConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        private const val USER_PREFERENCES = "user_preferences"

        private val KEY_AUDIOBOOKS_HOME = stringPreferencesKey("KEY_AUDIOBOOKS_HOME")

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

    suspend fun setAudiobooksHome(uri: Uri?) = dataStore.edit { preferences ->
        uri?.toString()?.let { preferences[KEY_AUDIOBOOKS_HOME] = it }
    }

    fun getAudiobooksHome(): Flow<Uri?> = dataStore.data.map { preferences ->
        preferences[KEY_AUDIOBOOKS_HOME]?.toUri()
    }

}
