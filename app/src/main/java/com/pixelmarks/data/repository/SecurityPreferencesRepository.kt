package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SecurityPreferencesRepository {
    fun observeAppLockEnabled(): Flow<Boolean>
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun isAppLockEnabled(): Boolean

    fun observeLockTimeout(): Flow<AppLockTimeout>
    suspend fun setLockTimeout(timeout: AppLockTimeout)
    suspend fun getLockTimeout(): AppLockTimeout
}

class SecurityPreferencesRepositoryImpl(
    private val context: Context
) : SecurityPreferencesRepository {

    companion object {
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_TIMEOUT = stringPreferencesKey("app_lock_timeout")
    }

    override fun observeAppLockEnabled(): Flow<Boolean> {
        return context.userPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[KEY_APP_LOCK_ENABLED] ?: false
            }
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun isAppLockEnabled(): Boolean {
        return observeAppLockEnabled().first()
    }

    override fun observeLockTimeout(): Flow<AppLockTimeout> {
        return context.userPreferencesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val raw = preferences[KEY_APP_LOCK_TIMEOUT]
                AppLockTimeout.fromString(raw)
            }
    }

    override suspend fun setLockTimeout(timeout: AppLockTimeout) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_TIMEOUT] = timeout.name
        }
    }

    override suspend fun getLockTimeout(): AppLockTimeout {
        return observeLockTimeout().first()
    }
}
