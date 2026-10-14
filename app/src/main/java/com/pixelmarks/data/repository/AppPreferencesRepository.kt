package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromString(value: String?): ThemeMode = when (value?.uppercase()) {
            "SYSTEM" -> SYSTEM
            "LIGHT" -> LIGHT
            else -> DARK
        }
    }
}

enum class AppColorScheme(val label: String, val hexColor: String) {
    ARCHIVAL_GOLD("Archival Gold", "#EAC07F"),
    TERRACOTTA("Terracotta Rose", "#F9B89A"),
    EMERALD("Emerald Forest", "#7EBA8C"),
    OBSIDIAN("Midnight Slate", "#A3B8CC");

    companion object {
        fun fromString(value: String?): AppColorScheme = when (value?.uppercase()) {
            "TERRACOTTA" -> TERRACOTTA
            "EMERALD" -> EMERALD
            "OBSIDIAN" -> OBSIDIAN
            else -> ARCHIVAL_GOLD
        }
    }
}

enum class DisplayFont(val label: String) {
    EDITORIAL_SERIF("Editorial (Serif)"),
    TECHNICAL_MONO("Technical (Mono)"),
    CLEAN_SANS("Clean (Sans-Serif)");

    companion object {
        fun fromString(value: String?): DisplayFont = when (value?.uppercase()) {
            "TECHNICAL_MONO" -> TECHNICAL_MONO
            "CLEAN_SANS" -> CLEAN_SANS
            else -> EDITORIAL_SERIF
        }
    }
}

enum class StartupPage(val label: String, val route: String) {
    HOME("Home", "home"),
    COLLECTIONS("Collections", "collections"),
    TAGS("Tags", "tags"),
    SEARCH("Search", "search");

    companion object {
        fun fromString(value: String?): StartupPage = when (value?.uppercase()) {
            "COLLECTIONS" -> COLLECTIONS
            "TAGS" -> TAGS
            "SEARCH" -> SEARCH
            else -> HOME
        }
    }
}

enum class CollectionStyle(val label: String) {
    FOLIO("Folio Cards"),
    GRID("Grid View"),
    LIST("Compact List");

    companion object {
        fun fromString(value: String?): CollectionStyle = when (value?.uppercase()) {
            "GRID" -> GRID
            "LIST" -> LIST
            else -> FOLIO
        }
    }
}

interface AppPreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun observeDynamicTheme(): Flow<Boolean>
    suspend fun setDynamicTheme(enabled: Boolean)

    fun observeColorScheme(): Flow<AppColorScheme>
    suspend fun setColorScheme(scheme: AppColorScheme)

    fun observeDisplayFont(): Flow<DisplayFont>
    suspend fun setDisplayFont(font: DisplayFont)

    fun observeLanguage(): Flow<String>
    suspend fun setLanguage(language: String)

    fun observeStartupPage(): Flow<StartupPage>
    suspend fun setStartupPage(page: StartupPage)

    fun observeCollectionStyle(): Flow<CollectionStyle>
    suspend fun setCollectionStyle(style: CollectionStyle)

    fun observeShowFavicon(): Flow<Boolean>
    suspend fun setShowFavicon(show: Boolean)

    fun observeShowDomain(): Flow<Boolean>
    suspend fun setShowDomain(show: Boolean)

    fun observeShowNotes(): Flow<Boolean>
    suspend fun setShowNotes(show: Boolean)

    fun observeCompactMode(): Flow<Boolean>
    suspend fun setCompactMode(compact: Boolean)
}

class AppPreferencesRepositoryImpl(
    private val context: Context
) : AppPreferencesRepository {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val KEY_DYNAMIC_THEME = booleanPreferencesKey("app_dynamic_theme")
        val KEY_COLOR_SCHEME = stringPreferencesKey("app_color_scheme")
        val KEY_DISPLAY_FONT = stringPreferencesKey("app_display_font")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_STARTUP_PAGE = stringPreferencesKey("app_startup_page")
        val KEY_COLLECTION_STYLE = stringPreferencesKey("app_collection_style")
        val KEY_SHOW_FAVICON = booleanPreferencesKey("bookmark_show_favicon")
        val KEY_SHOW_DOMAIN = booleanPreferencesKey("bookmark_show_domain")
        val KEY_SHOW_NOTES = booleanPreferencesKey("bookmark_show_notes")
        val KEY_COMPACT_MODE = booleanPreferencesKey("bookmark_compact_mode")
    }

    private val safeData = context.userPreferencesDataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    override fun observeThemeMode(): Flow<ThemeMode> = safeData.map { prefs ->
        ThemeMode.fromString(prefs[KEY_THEME_MODE])
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override fun observeDynamicTheme(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_DYNAMIC_THEME] ?: false
    }

    override suspend fun setDynamicTheme(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_DYNAMIC_THEME] = enabled }
    }

    override fun observeColorScheme(): Flow<AppColorScheme> = safeData.map { prefs ->
        AppColorScheme.fromString(prefs[KEY_COLOR_SCHEME])
    }

    override suspend fun setColorScheme(scheme: AppColorScheme) {
        context.userPreferencesDataStore.edit { it[KEY_COLOR_SCHEME] = scheme.name }
    }

    override fun observeDisplayFont(): Flow<DisplayFont> = safeData.map { prefs ->
        DisplayFont.fromString(prefs[KEY_DISPLAY_FONT])
    }

    override suspend fun setDisplayFont(font: DisplayFont) {
        context.userPreferencesDataStore.edit { it[KEY_DISPLAY_FONT] = font.name }
    }

    override fun observeLanguage(): Flow<String> = safeData.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "English"
    }

    override suspend fun setLanguage(language: String) {
        context.userPreferencesDataStore.edit { it[KEY_LANGUAGE] = language }
    }

    override fun observeStartupPage(): Flow<StartupPage> = safeData.map { prefs ->
        StartupPage.fromString(prefs[KEY_STARTUP_PAGE])
    }

    override suspend fun setStartupPage(page: StartupPage) {
        context.userPreferencesDataStore.edit { it[KEY_STARTUP_PAGE] = page.name }
    }

    override fun observeCollectionStyle(): Flow<CollectionStyle> = safeData.map { prefs ->
        CollectionStyle.fromString(prefs[KEY_COLLECTION_STYLE])
    }

    override suspend fun setCollectionStyle(style: CollectionStyle) {
        context.userPreferencesDataStore.edit { it[KEY_COLLECTION_STYLE] = style.name }
    }

    override fun observeShowFavicon(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_FAVICON] ?: true
    }

    override suspend fun setShowFavicon(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_FAVICON] = show }
    }

    override fun observeShowDomain(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_DOMAIN] ?: true
    }

    override suspend fun setShowDomain(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_DOMAIN] = show }
    }

    override fun observeShowNotes(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_NOTES] ?: true
    }

    override suspend fun setShowNotes(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_NOTES] = show }
    }

    override fun observeCompactMode(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_COMPACT_MODE] ?: false
    }

    override suspend fun setCompactMode(compact: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_COMPACT_MODE] = compact }
    }
}
