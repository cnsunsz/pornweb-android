package com.pornweb.android.data

import android.content.Context

class ServerStore(context: Context) {
    private val prefs = context.getSharedPreferences("pw_server", Context.MODE_PRIVATE)

    var baseUrl: String
        get() {
            val raw = prefs.getString(KEY_URL, null)?.trim().orEmpty()
            // One-shot migrate: previous shipped defaults → current host
            val legacy = raw.trimEnd('/')
            if (raw.isBlank() || legacy in LEGACY_DEFAULT_URLS) {
                if (raw.isNotBlank()) {
                    prefs.edit().putString(KEY_URL, DEFAULT_URL).apply()
                }
                return DEFAULT_URL
            }
            return raw
        }
        set(value) {
            prefs.edit().putString(KEY_URL, normalize(value)).apply()
        }

    var connected: Boolean
        get() = prefs.getBoolean(KEY_CONNECTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONNECTED, value).apply()
        }

    fun normalizedBase(): String = normalize(baseUrl)

    companion object {
        const val DEFAULT_URL = "http://web.cnsz.kdns.fr"
        private val LEGACY_DEFAULT_URLS = setOf(
            "http://43.196.70.121:10086",
            "http://web.cnsun.top:2052",
            "http://web.cnsz.kdns.fr:2052",
        )
        private const val KEY_URL = "base_url"
        private const val KEY_CONNECTED = "connected"

        fun normalize(raw: String): String {
            var u = raw.trim()
            if (u.isEmpty()) u = DEFAULT_URL
            if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = "http://$u"
            }
            return u.trimEnd('/')
        }
    }
}
