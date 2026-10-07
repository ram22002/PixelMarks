package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelmarks.core.security.CustomPasswordManager
import com.pixelmarks.core.security.model.PrivateAuthMethod
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.intPreferencesKey

interface PrivatePreferencesRepository {
    val authMethod: Flow<PrivateAuthMethod>
    val hasCustomPassword: Flow<Boolean>
    val passwordHint: Flow<String?>
    val isRecoveryEnabled: Flow<Boolean>

    suspend fun setAuthMethod(method: PrivateAuthMethod)
    suspend fun setCustomPassword(password: CharArray, hint: String?)
    suspend fun updateCustomPassword(newPassword: CharArray, newHint: String?)
    suspend fun clearCustomPassword()
    suspend fun setRecoveryEnabled(enabled: Boolean)
    suspend fun verifyCustomPassword(password: CharArray): Boolean
    suspend fun getPasswordHint(): String?
}

class PrivatePreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PrivatePreferencesRepository {

    constructor(context: Context) : this(context.userPreferencesDataStore)

    private object PreferencesKeys {
        val AUTH_METHOD = stringPreferencesKey("secure_folder_auth_method")
        val PASSWORD_SALT = stringPreferencesKey("secure_folder_pwd_salt")
        val PASSWORD_VERIFIER = stringPreferencesKey("secure_folder_pwd_verifier")
        val PASSWORD_HINT = stringPreferencesKey("secure_folder_pwd_hint")
        val PASSWORD_KDF_VERSION = intPreferencesKey("secure_folder_pwd_kdf_version")
        val RECOVERY_ENABLED = booleanPreferencesKey("secure_folder_recovery_enabled")
    }

    override val authMethod: Flow<PrivateAuthMethod> = dataStore.data.map { preferences ->
        when (preferences[PreferencesKeys.AUTH_METHOD]) {
            PrivateAuthMethod.DEVICE_CREDENTIAL.name -> PrivateAuthMethod.DEVICE_CREDENTIAL
            PrivateAuthMethod.CUSTOM_PASSWORD.name -> PrivateAuthMethod.CUSTOM_PASSWORD
            else -> PrivateAuthMethod.BIOMETRIC
        }
    }

    override val hasCustomPassword: Flow<Boolean> = dataStore.data.map { preferences ->
        !preferences[PreferencesKeys.PASSWORD_VERIFIER].isNullOrBlank()
    }

    override val passwordHint: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PASSWORD_HINT]
    }

    override val isRecoveryEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.RECOVERY_ENABLED] ?: true
    }

    override suspend fun setAuthMethod(method: PrivateAuthMethod) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_METHOD] = method.name
        }
    }

    override suspend fun setCustomPassword(password: CharArray, hint: String?) {
        val saltBytes = CustomPasswordManager.generateSalt()
        val saltHex = CustomPasswordManager.bytesToHex(saltBytes)
        val verifierHex = CustomPasswordManager.deriveKeyHex(
            password = password,
            salt = saltBytes,
            iterations = CustomPasswordManager.ITERATIONS_V2
        )

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PASSWORD_SALT] = saltHex
            preferences[PreferencesKeys.PASSWORD_VERIFIER] = verifierHex
            preferences[PreferencesKeys.PASSWORD_KDF_VERSION] = CustomPasswordManager.CURRENT_KDF_VERSION
            if (!hint.isNullOrBlank() && CustomPasswordManager.isHintAcceptable(password, hint)) {
                preferences[PreferencesKeys.PASSWORD_HINT] = hint.trim()
            } else {
                preferences.remove(PreferencesKeys.PASSWORD_HINT)
            }
            preferences[PreferencesKeys.AUTH_METHOD] = PrivateAuthMethod.CUSTOM_PASSWORD.name
        }
    }

    override suspend fun updateCustomPassword(newPassword: CharArray, newHint: String?) {
        setCustomPassword(newPassword, newHint)
    }

    override suspend fun clearCustomPassword() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PASSWORD_SALT)
            preferences.remove(PreferencesKeys.PASSWORD_VERIFIER)
            preferences.remove(PreferencesKeys.PASSWORD_HINT)
            preferences.remove(PreferencesKeys.PASSWORD_KDF_VERSION)
            if (preferences[PreferencesKeys.AUTH_METHOD] == PrivateAuthMethod.CUSTOM_PASSWORD.name) {
                preferences[PreferencesKeys.AUTH_METHOD] = PrivateAuthMethod.BIOMETRIC.name
            }
        }
    }

    override suspend fun setRecoveryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECOVERY_ENABLED] = enabled
        }
    }

    override suspend fun verifyCustomPassword(password: CharArray): Boolean {
        val prefs = dataStore.data.first()
        val saltHex = prefs[PreferencesKeys.PASSWORD_SALT] ?: return false
        val verifierHex = prefs[PreferencesKeys.PASSWORD_VERIFIER] ?: return false
        val kdfVersion = prefs[PreferencesKeys.PASSWORD_KDF_VERSION] ?: CustomPasswordManager.KDF_VERSION_1

        val isValid = CustomPasswordManager.verifyPassword(password, saltHex, verifierHex, kdfVersion)
        if (isValid && kdfVersion < CustomPasswordManager.CURRENT_KDF_VERSION) {
            // Transparently upgrade legacy KDF hash to v2
            val currentHint = prefs[PreferencesKeys.PASSWORD_HINT]
            setCustomPassword(password, currentHint)
        }
        return isValid
    }

    override suspend fun getPasswordHint(): String? {
        val prefs = dataStore.data.first()
        return prefs[PreferencesKeys.PASSWORD_HINT]
    }
}
