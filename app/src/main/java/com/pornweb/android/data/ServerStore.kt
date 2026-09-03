package com.pornweb.android.data

import android.content.Context

class ServerStore(context: Context) {
    private val prefs = context.getSharedPreferences("pw_server", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL)?.trim().orEmpty().ifBlank { DEFAULT_URL }
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
        const val DEFAULT_URL = "http://43.196.70.121:10086"
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
