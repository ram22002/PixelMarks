package com.pixelmarks.core.browser

/**
 * User preference for how web links and bookmarks are opened.
 */
enum class BrowserPreference {
    IN_APP,
    SYSTEM_PREFERRED,
    DEFAULT_BROWSER,
    ASK_EVERY_TIME;

    companion object {
        val DEFAULT = SYSTEM_PREFERRED

        /**
         * Safely deserializes a persisted string into a [BrowserPreference],
         * falling back to [SYSTEM_PREFERRED] if unknown or corrupt.
         */
        fun fromString(value: String?): BrowserPreference {
            return when (value?.trim()) {
                "IN_APP" -> IN_APP
                "SYSTEM_PREFERRED" -> SYSTEM_PREFERRED
                "DEFAULT_BROWSER" -> DEFAULT_BROWSER
                "ASK_EVERY_TIME" -> ASK_EVERY_TIME
                else -> DEFAULT
            }
        }
    }
}
