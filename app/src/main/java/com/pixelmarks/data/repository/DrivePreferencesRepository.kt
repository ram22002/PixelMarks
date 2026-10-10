package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.AutoBackupPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.driveDataStore: DataStore<Preferences> by preferencesDataStore(name = "drive_preferences")

interface DrivePreferencesRepository {
    val autoBackupPreferences: Flow<AutoBackupPreferences>
    val connectedAccountEmail: Flow<String?>
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency)
    suspend fun setWifiOnly(wifiOnly: Boolean)
    suspend fun updateLastCloudBackup(time: Long, name: String)
    suspend fun setConnectedAccountEmail(email: String?)
    suspend fun clear()
}

class DrivePreferencesRepositoryImpl(
    private val context: Context
) : DrivePreferencesRepository {

    companion object {
        val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val KEY_AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        val KEY_WIFI_ONLY = booleanPreferencesKey("auto_backup_wifi_only")
        val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_cloud_backup_time")
        val KEY_LAST_BACKUP_NAME = stringPreferencesKey("last_cloud_backup_name")
        val KEY_CONNECTED_EMAIL = stringPreferencesKey("connected_account_email")
    }

    override val autoBackupPreferences: Flow<AutoBackupPreferences> = context.driveDataStore.data.map { prefs ->
        val enabled = prefs[KEY_AUTO_BACKUP_ENABLED] ?: false
        val freqStr = prefs[KEY_AUTO_BACKUP_FREQUENCY]
        val frequency = AutoBackupFrequency.fromString(freqStr)
        val wifiOnly = prefs[KEY_WIFI_ONLY] ?: true
        val lastTime = prefs[KEY_LAST_BACKUP_TIME]

        AutoBackupPreferences(
            enabled = enabled,
            frequency = frequency,
            wifiOnly = wifiOnly,
            lastAutoBackupTime = lastTime
        )
    }

    override val connectedAccountEmail: Flow<String?> = context.driveDataStore.data.map { prefs ->
        prefs[KEY_CONNECTED_EMAIL]
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.driveDataStore.edit { prefs ->
            prefs[KEY_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        context.driveDataStore.edit { prefs ->
            prefs[KEY_AUTO_BACKUP_FREQUENCY] = frequency.name
        }
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.driveDataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY] = wifiOnly
        }
    }

    override suspend fun updateLastCloudBackup(time: Long, name: String) {
        context.driveDataStore.edit { prefs ->
            prefs[KEY_LAST_BACKUP_TIME] = time
            prefs[KEY_LAST_BACKUP_NAME] = name
        }
    }

    override suspend fun setConnectedAccountEmail(email: String?) {
        context.driveDataStore.edit { prefs ->
            if (email != null) {
                prefs[KEY_CONNECTED_EMAIL] = email
            } else {
                prefs.remove(KEY_CONNECTED_EMAIL)
            }
        }
    }

    override suspend fun clear() {
        context.driveDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
