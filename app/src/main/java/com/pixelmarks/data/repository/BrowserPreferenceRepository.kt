package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

interface BrowserPreferenceRepository {
    fun observePreference(): Flow<BrowserPreference>
    suspend fun setPreference(preference: BrowserPreference)
    suspend fun getPreference(): BrowserPreference
}

class BrowserPreferenceRepositoryImpl(
    private val context: Context
) : BrowserPreferenceRepository {

    companion object {
        val KEY_BROWSER_PREFERENCE = stringPreferencesKey("browser_preference")
    }

    override fun observePreference(): Flow<BrowserPreference> {
        return context.userPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val raw = preferences[KEY_BROWSER_PREFERENCE]
                BrowserPreference.fromString(raw)
            }
    }

    override suspend fun setPreference(preference: BrowserPreference) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_BROWSER_PREFERENCE] = preference.name
        }
    }

    override suspend fun getPreference(): BrowserPreference {
        return observePreference().first()
    }
}
