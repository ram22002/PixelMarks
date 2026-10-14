package com.pixelmarks.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("Pure Black (AMOLED)");

    companion object {
        fun fromString(value: String?): ThemeMode = when (value?.uppercase()) {
            "SYSTEM" -> SYSTEM
            "LIGHT" -> LIGHT
            "AMOLED" -> AMOLED
            else -> DARK
        }
    }
}

enum class AppColorScheme(
    val label: String,
    val accentHex: String,
    val primaryHex: String,
    val secondaryHex: String,
    val tertiaryHex: String
) {
    ROSE_GOLD("Rose & Sand", "#592534", "#F0A2A0", "#E7CA9E", "#F0BDD8"),
    CRIMSON_GOLD("Maroon & Peach", "#732034", "#F1B5AC", "#E9CCA4", "#E8B0C3"),
    PURPLE_PEACH("Plum & Lilac", "#4C2958", "#D5BDDC", "#F1B4BD", "#C5CFE8"),
    INDIGO_PINK("Indigo & Periwinkle", "#32386A", "#BCC3E9", "#F0ADC3", "#D4C2EA"),
    NAVY_LAVENDER("Midnight & Sky", "#1F3563", "#B3C6E9", "#E5B6D2", "#C0DCDE"),
    ARCHIVAL_GOLD("Archival Gold", "#5A4218", "#EAC07F", "#F9B89A", "#C2D9B8"),
    EMERALD_SAGE("Emerald & Sage", "#1E4A28", "#88C995", "#B2DFDB", "#D7E3B6");

    val hexColor: String get() = primaryHex

    companion object {
        val TERRACOTTA = ROSE_GOLD
        val EMERALD = EMERALD_SAGE
        val OBSIDIAN = NAVY_LAVENDER

        fun fromString(value: String?): AppColorScheme = when (value?.uppercase()) {
            "ROSE_GOLD", "TERRACOTTA" -> ROSE_GOLD
            "CRIMSON_GOLD" -> CRIMSON_GOLD
            "PURPLE_PEACH" -> PURPLE_PEACH
            "INDIGO_PINK" -> INDIGO_PINK
            "NAVY_LAVENDER", "OBSIDIAN" -> NAVY_LAVENDER
            "EMERALD_SAGE", "EMERALD" -> EMERALD_SAGE
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

    // Legacy card flags
    fun observeShowFavicon(): Flow<Boolean>
    suspend fun setShowFavicon(show: Boolean)
    fun observeShowDomain(): Flow<Boolean>
    suspend fun setShowDomain(show: Boolean)
    fun observeShowNotes(): Flow<Boolean>
    suspend fun setShowNotes(show: Boolean)
    fun observeCompactMode(): Flow<Boolean>
    suspend fun setCompactMode(compact: Boolean)

    // Full Bookmark Customization (Screenshot 1)
    fun observeLayoutDisplayMode(): Flow<String>
    suspend fun setLayoutDisplayMode(mode: String)

    fun observeShowDomainThumbnail(): Flow<Boolean>
    suspend fun setShowDomainThumbnail(show: Boolean)

    fun observeShowTags(): Flow<Boolean>
    suspend fun setShowTags(show: Boolean)

    fun observeMaxTagsToShow(): Flow<Int>
    suspend fun setMaxTagsToShow(max: Int)

    fun observeShowCreationTime(): Flow<Boolean>
    suspend fun setShowCreationTime(show: Boolean)

    fun observeCreationTimeFormat(): Flow<Int>
    suspend fun setCreationTimeFormat(format: Int)

    fun observeShowTitle(): Flow<Boolean>
    suspend fun setShowTitle(show: Boolean)

    fun observeTitleMaxLines(): Flow<Int>
    suspend fun setTitleMaxLines(lines: Int)

    fun observeShowSubtitle(): Flow<Boolean>
    suspend fun setShowSubtitle(show: Boolean)

    fun observeSubtitleMaxLines(): Flow<Int>
    suspend fun setSubtitleMaxLines(lines: Int)

    fun observeShowSiteName(): Flow<Boolean>
    suspend fun setShowSiteName(show: Boolean)

    fun observeShowTimeIcons(): Flow<Boolean>
    suspend fun setShowTimeIcons(show: Boolean)

    fun observeShowPinIcon(): Flow<Boolean>
    suspend fun setShowPinIcon(show: Boolean)

    fun observeShowNote(): Flow<Boolean>
    suspend fun setShowNote(show: Boolean)

    fun observeNoteMaxLines(): Flow<Int>
    suspend fun setNoteMaxLines(lines: Int)

    suspend fun resetAllBookmarkSettings()
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

        // Screenshot 1 preference keys
        val KEY_LAYOUT_DISPLAY_MODE = stringPreferencesKey("bm_layout_display_mode")
        val KEY_SHOW_DOMAIN_THUMBNAIL = booleanPreferencesKey("bm_show_domain_thumbnail")
        val KEY_SHOW_TAGS = booleanPreferencesKey("bm_show_tags")
        val KEY_MAX_TAGS_TO_SHOW = intPreferencesKey("bm_max_tags_to_show")
        val KEY_SHOW_CREATION_TIME = booleanPreferencesKey("bm_show_creation_time")
        val KEY_CREATION_TIME_FORMAT = intPreferencesKey("bm_creation_time_format")
        val KEY_SHOW_TITLE = booleanPreferencesKey("bm_show_title")
        val KEY_TITLE_MAX_LINES = intPreferencesKey("bm_title_max_lines")
        val KEY_SHOW_SUBTITLE = booleanPreferencesKey("bm_show_subtitle")
        val KEY_SUBTITLE_MAX_LINES = intPreferencesKey("bm_subtitle_max_lines")
        val KEY_SHOW_SITE_NAME = booleanPreferencesKey("bm_show_site_name")
        val KEY_SHOW_TIME_ICONS = booleanPreferencesKey("bm_show_time_icons")
        val KEY_SHOW_PIN_ICON = booleanPreferencesKey("bm_show_pin_icon")
        val KEY_SHOW_NOTE = booleanPreferencesKey("bm_show_note")
        val KEY_NOTE_MAX_LINES = intPreferencesKey("bm_note_max_lines")
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

    // Screenshot 1 implementation
    override fun observeLayoutDisplayMode(): Flow<String> = safeData.map { prefs ->
        prefs[KEY_LAYOUT_DISPLAY_MODE] ?: "List"
    }

    override suspend fun setLayoutDisplayMode(mode: String) {
        context.userPreferencesDataStore.edit { it[KEY_LAYOUT_DISPLAY_MODE] = mode }
    }

    override fun observeShowDomainThumbnail(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_DOMAIN_THUMBNAIL] ?: true
    }

    override suspend fun setShowDomainThumbnail(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_DOMAIN_THUMBNAIL] = show }
    }

    override fun observeShowTags(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_TAGS] ?: true
    }

    override suspend fun setShowTags(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_TAGS] = show }
    }

    override fun observeMaxTagsToShow(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_MAX_TAGS_TO_SHOW] ?: 4
    }

    override suspend fun setMaxTagsToShow(max: Int) {
        context.userPreferencesDataStore.edit { it[KEY_MAX_TAGS_TO_SHOW] = max.coerceIn(1, 10) }
    }

    override fun observeShowCreationTime(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_CREATION_TIME] ?: true
    }

    override suspend fun setShowCreationTime(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_CREATION_TIME] = show }
    }

    override fun observeCreationTimeFormat(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_CREATION_TIME_FORMAT] ?: 2
    }

    override suspend fun setCreationTimeFormat(format: Int) {
        context.userPreferencesDataStore.edit { it[KEY_CREATION_TIME_FORMAT] = format.coerceIn(1, 3) }
    }

    override fun observeShowTitle(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_TITLE] ?: true
    }

    override suspend fun setShowTitle(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_TITLE] = show }
    }

    override fun observeTitleMaxLines(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_TITLE_MAX_LINES] ?: 1
    }

    override suspend fun setTitleMaxLines(lines: Int) {
        context.userPreferencesDataStore.edit { it[KEY_TITLE_MAX_LINES] = lines.coerceIn(1, 4) }
    }

    override fun observeShowSubtitle(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_SUBTITLE] ?: true
    }

    override suspend fun setShowSubtitle(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_SUBTITLE] = show }
    }

    override fun observeSubtitleMaxLines(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_SUBTITLE_MAX_LINES] ?: 1
    }

    override suspend fun setSubtitleMaxLines(lines: Int) {
        context.userPreferencesDataStore.edit { it[KEY_SUBTITLE_MAX_LINES] = lines.coerceIn(1, 4) }
    }

    override fun observeShowSiteName(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_SITE_NAME] ?: true
    }

    override suspend fun setShowSiteName(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_SITE_NAME] = show }
    }

    override fun observeShowTimeIcons(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_TIME_ICONS] ?: true
    }

    override suspend fun setShowTimeIcons(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_TIME_ICONS] = show }
    }

    override fun observeShowPinIcon(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_PIN_ICON] ?: true
    }

    override suspend fun setShowPinIcon(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_PIN_ICON] = show }
    }

    override fun observeShowNote(): Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_SHOW_NOTE] ?: true
    }

    override suspend fun setShowNote(show: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_NOTE] = show }
    }

    override fun observeNoteMaxLines(): Flow<Int> = safeData.map { prefs ->
        prefs[KEY_NOTE_MAX_LINES] ?: 1
    }

    override suspend fun setNoteMaxLines(lines: Int) {
        context.userPreferencesDataStore.edit { it[KEY_NOTE_MAX_LINES] = lines.coerceIn(1, 4) }
    }

    override suspend fun resetAllBookmarkSettings() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_LAYOUT_DISPLAY_MODE] = "List"
            prefs[KEY_SHOW_DOMAIN_THUMBNAIL] = true
            prefs[KEY_SHOW_TAGS] = true
            prefs[KEY_MAX_TAGS_TO_SHOW] = 4
            prefs[KEY_SHOW_CREATION_TIME] = true
            prefs[KEY_CREATION_TIME_FORMAT] = 2
            prefs[KEY_SHOW_TITLE] = true
            prefs[KEY_TITLE_MAX_LINES] = 1
            prefs[KEY_SHOW_SUBTITLE] = true
            prefs[KEY_SUBTITLE_MAX_LINES] = 1
            prefs[KEY_SHOW_SITE_NAME] = true
            prefs[KEY_SHOW_TIME_ICONS] = true
            prefs[KEY_SHOW_PIN_ICON] = true
            prefs[KEY_SHOW_NOTE] = true
            prefs[KEY_NOTE_MAX_LINES] = 1
            prefs[KEY_SHOW_FAVICON] = true
            prefs[KEY_SHOW_DOMAIN] = true
            prefs[KEY_SHOW_NOTES] = true
            prefs[KEY_COMPACT_MODE] = false
        }
    }
}
