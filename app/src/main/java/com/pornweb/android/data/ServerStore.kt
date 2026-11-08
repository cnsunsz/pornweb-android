package com.pornweb.android.data

import android.content.Context

class ServerStore(context: Context) {
    private val prefs = context.getSharedPreferences("pw_server", Context.MODE_PRIVATE)

    var baseUrl: String
        get() {
            val raw = prefs.getString(KEY_URL, null)?.trim().orEmpty()
            val legacy = raw.trimEnd('/')
            // Lock to DEFAULT_URL; rewrite blank / legacy / any custom stored URL.
            if (raw.isNotBlank() && legacy != DEFAULT_URL) {
                prefs.edit().putString(KEY_URL, DEFAULT_URL).apply()
            }
            return DEFAULT_URL
        }
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Server address is fixed; ignore edits and persist DEFAULT_URL only.
            prefs.edit().putString(KEY_URL, DEFAULT_URL).apply()
        }

    var connected: Boolean
        get() = prefs.getBoolean(KEY_CONNECTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONNECTED, value).apply()
        }

    fun normalizedBase(): String = normalize(baseUrl)

    companion object {
        const val DEFAULT_URL = "https://web.cnsz.kdns.fr"
        /** Previous shipped defaults rewritten to [DEFAULT_URL] on read. */
        private val LEGACY_DEFAULT_URLS = setOf(
            "http://43.196.70.121:10086",
            "http://web.cnsun.top:2052",
            "http://web.cnsz.kdns.fr",
            "http://web.cnsz.kdns.fr:2052",
            "https://web.cnsz.kdns.fr:2052",
        )
        private const val KEY_URL = "base_url"
        private const val KEY_CONNECTED = "connected"

        fun normalize(raw: String): String {
            var u = raw.trim()
            if (u.isEmpty()) u = DEFAULT_URL
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "https://$u"
            }
            val trimmed = u.trimEnd('/')
            if (trimmed in LEGACY_DEFAULT_URLS) return DEFAULT_URL
            return trimmed
        }
    }
}
