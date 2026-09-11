package com.pornweb.android.data

import android.content.Context

/**
 * Playback preferences inspired by VidHub / MX Player / KMPlayer / Emby / Jellyfin.
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

    /** Auto-hide player controls after this many milliseconds while playing. */
    var autoHideControlsMs: Int
        get() = prefs.getInt(KEY_AUTO_HIDE_MS, 4_000).coerceIn(1_000, 30_000)
        set(value) { prefs.edit().putInt(KEY_AUTO_HIDE_MS, value.coerceIn(1_000, 30_000)).apply() }

    /** Show remaining time (−mm:ss) instead of total duration on the scrubber. */
    var showRemainingTime: Boolean
        get() = prefs.getBoolean(KEY_SHOW_REMAINING, false)
        set(value) { prefs.edit().putBoolean(KEY_SHOW_REMAINING, value).apply() }

    /** Persist last chosen playback speed across sessions. */
    var rememberSpeed: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_SPEED, true)
        set(value) { prefs.edit().putBoolean(KEY_REMEMBER_SPEED, value).apply() }

    /** Last speed chosen in the player speed menu (used when rememberSpeed is on). */
    var lastSpeed: Float
        get() = prefs.getFloat(KEY_LAST_SPEED, defaultSpeed)
        set(value) { prefs.edit().putFloat(KEY_LAST_SPEED, value).apply() }

    /** When a part ends, automatically start the next part if available. */
    var continuousPlayNextPart: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_NEXT, true)
        set(value) { prefs.edit().putBoolean(KEY_CONTINUOUS_NEXT, value).apply() }

    /** Effective startup speed: lastSpeed if rememberSpeed, else defaultSpeed. */
    fun effectiveSpeed(): Float =
        if (rememberSpeed) lastSpeed.coerceIn(0.25f, 4.0f) else defaultSpeed.coerceIn(0.25f, 4.0f)

    companion object {
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_LONG_PRESS_SPEED = "long_press_speed"
        private const val KEY_SKIP_SECONDS = "skip_seconds"
        private const val KEY_SWIPE_SEEK = "swipe_seek"
        private const val KEY_START_LANDSCAPE = "start_landscape"
        private const val KEY_DOUBLE_TAP = "double_tap"
        private const val KEY_LEFT_REWIND = "left_rewind"
        private const val KEY_RESUME = "resume_on_open"
        private const val KEY_AUTO_HIDE_MS = "auto_hide_ms"
        private const val KEY_SHOW_REMAINING = "show_remaining"
        private const val KEY_REMEMBER_SPEED = "remember_speed"
        private const val KEY_LAST_SPEED = "last_speed"
        private const val KEY_CONTINUOUS_NEXT = "continuous_next_part"
    }
}
