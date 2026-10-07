package com.pixelmarks.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Shared singleton Preferences DataStore for PixelMarks application settings.
 */
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelmarks_user_preferences")
