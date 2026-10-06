package com.pixelmarks.core.util

/**
 * Utility functions for search queries and SQL LIKE pattern escaping.
 */
object SearchQueryUtils {

    /**
     * Escapes wildcard characters for SQLite LIKE queries with ESCAPE '\'.
     */
    fun escapeSqlLike(query: String): String {
        return query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
