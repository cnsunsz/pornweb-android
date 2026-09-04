package com.pornweb.android.data

import android.content.Context

/**
 * Playback preferences inspired by MX Player / KMPlayer / Emby / Jellyfin.
 */
class PlayerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("pw_player", Context.MODE_PRIVATE)

    var defaultSpeed: Float
        get() = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f)
        set(value) { prefs.edit().putFloat(KEY_DEFAULT_SPEED, value).apply() }

    /** Temporary speed while long-pressing (MX / KMPlayer). */
    var longPressSpeed: Float
        get() = prefs.getFloat(KEY_LONG_PRESS_SPEED, 2.0f)
        set(value) { prefs.edit().putFloat(KEY_LONG_PRESS_SPEED, value).apply() }

    /** Double-tap / skip buttons seconds. */
    var skipSeconds: Int
        get() = prefs.getInt(KEY_SKIP_SECONDS, 10)
        set(value) { prefs.edit().putInt(KEY_SKIP_SECONDS, value).apply() }

    /** Full-width swipe ≈ this many seconds of seek. */
    var swipeSeekSeconds: Int
        get() = prefs.getInt(KEY_SWIPE_SEEK, 90)
        set(value) { prefs.edit().putInt(KEY_SWIPE_SEEK, value).apply() }

    var startLandscape: Boolean
        get() = prefs.getBoolean(KEY_START_LANDSCAPE, false)
        set(value) { prefs.edit().putBoolean(KEY_START_LANDSCAPE, value).apply() }

    var doubleTapSeek: Boolean
        get() = prefs.getBoolean(KEY_DOUBLE_TAP, true)
        set(value) { prefs.edit().putBoolean(KEY_DOUBLE_TAP, value).apply() }

    var leftLongPressRewind: Boolean
        get() = prefs.getBoolean(KEY_LEFT_REWIND, true)
        set(value) { prefs.edit().putBoolean(KEY_LEFT_REWIND, value).apply() }

    var resumeOnOpen: Boolean
        get() = prefs.getBoolean(KEY_RESUME, true)
        set(value) { prefs.edit().putBoolean(KEY_RESUME, value).apply() }

    companion object {
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_LONG_PRESS_SPEED = "long_press_speed"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_SWIPE_SEEK = "swipe_seek"
        private const val KEY_START_LANDSCAPE = "start_landscape"
        private const val KEY_DOUBLE_TAP = "double_tap"
        private const val KEY_LEFT_REWIND = "left_rewind"
        private const val KEY_RESUME = "resume_on_open"
    }
}
