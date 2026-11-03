package com.pixelmarks.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.updateDataStore by preferencesDataStore(name = "update_preferences")

/**
 * Persists update-checking state using DataStore:
 * - The version name of the last dismissed update (so we don't re-prompt for it).
 * - The timestamp of the last successful update check (to throttle startup checks).
 * - The user's preferred release channel (stable / beta / nightly).
 */
class UpdatePreferences(private val context: Context) {

    companion object {
        private val KEY_DISMISSED_VERSION = stringPreferencesKey("dismissed_update_version")
        private val KEY_LAST_CHECK_TS = longPreferencesKey("last_update_check_timestamp")
        private val KEY_RELEASE_CHANNEL = stringPreferencesKey("release_channel")

        /** Minimum interval between automatic startup checks: 6 hours. */
        const val MIN_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }

    /** Observe the version name the user has dismissed. */
    val dismissedVersion: Flow<String?> = context.updateDataStore.data.map { prefs ->
        prefs[KEY_DISMISSED_VERSION]
    }

    /** Observe the timestamp of the last update check. */
    val lastCheckTimestamp: Flow<Long> = context.updateDataStore.data.map { prefs ->
        prefs[KEY_LAST_CHECK_TS] ?: 0L
    }

    /** Observe the user's selected release channel name (default: "STABLE"). */
    val releaseChannelName: Flow<String> = context.updateDataStore.data.map { prefs ->
        prefs[KEY_RELEASE_CHANNEL] ?: "STABLE"
    }

    /** Persist the version the user dismissed, preventing re-prompt for that version. */
    suspend fun setDismissedVersion(versionName: String) {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_DISMISSED_VERSION] = versionName
        }
    }

    /** Clear the dismissed version (e.g. when a newer version supersedes it). */
    suspend fun clearDismissedVersion() {
        context.updateDataStore.edit { prefs ->
            prefs.remove(KEY_DISMISSED_VERSION)
        }
    }

    /** Record the timestamp of a completed update check. */
    suspend fun recordCheckTimestamp() {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_LAST_CHECK_TS] = System.currentTimeMillis()
        }
    }

    /** Persist the user's channel preference. */
    suspend fun setReleaseChannel(channelName: String) {
        context.updateDataStore.edit { prefs ->
            prefs[KEY_RELEASE_CHANNEL] = channelName
        }
    }
}

