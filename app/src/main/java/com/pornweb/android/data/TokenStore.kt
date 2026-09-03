package com.pornweb.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStore(context: Context) {
    private val gson = Gson()
    private val prefs: SharedPreferences = createPrefs(context)

    private val _token = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val tokenFlow: StateFlow<String?> = _token.asStateFlow()

    private val _user = MutableStateFlow(readUser())
    val userFlow: StateFlow<User?> = _user.asStateFlow()

    var token: String?
        get() = _token.value
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value).apply()
            _token.value = value
        }

    var user: User?
        get() = _user.value
        set(value) {
            if (value == null) prefs.edit().remove(KEY_USER).apply()
            else prefs.edit().putString(KEY_USER, gson.toJson(value)).apply()
            _user.value = value
        }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply()
        _token.value = null
        _user.value = null
    }

    fun hasToken(): Boolean = !token.isNullOrBlank()

    private fun readUser(): User? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(raw, User::class.java)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "TokenStore"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_USER = "user_json"
        private const val FILE_ENC = "pw_secure_prefs"
        private const val FILE_PLAIN = "pw_prefs"

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    FILE_ENC,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back", e)
                context.getSharedPreferences(FILE_PLAIN, Context.MODE_PRIVATE)
            }
        }
    }
}
