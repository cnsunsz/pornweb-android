package com.pornweb.android.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class HealthResponse(
    val status: String? = null,
    val app: String? = null
)

data class User(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean? = null,
    val avatar: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val user: User? = null
)

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(val username: String, val email: String, val password: String)

data class PasswordChangeRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class ProgressRequest(
    val position: Double,
    val duration: Double,
    val part: Int = 0
)

data class ExtraFile(
    val label: String? = null,
    val path: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class MediaItem(
    val id: Long? = null,
    val title: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val rating: Double? = null,
    val director: String? = null,
    @SerializedName("cast_list") val castList: String? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    @SerializedName("fanart_url") val fanartUrl: String? = null,
    val category: String? = null,
    val filename: String? = null,
    @SerializedName("file_size") val fileSize: Long? = null,
    val folder: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("extra_files") val extraFilesRaw: JsonElement? = null,
    val duration: Double? = null,
    val progress: Double? = null,
    @SerializedName("progress_part") val progressPart: Int? = null
) {
    fun mediaId(): Long = id ?: 0L

    fun extraFileList(): List<ExtraFile> {
        val raw = extraFilesRaw ?: return emptyList()
        val gson = com.google.gson.Gson()
        try {
            if (raw.isJsonArray) {
                return raw.asJsonArray.mapNotNull {
                    try { gson.fromJson(it, ExtraFile::class.java) } catch (_: Exception) { null }
                }
            }
            if (raw.isJsonPrimitive) {
                val s = raw.asString.trim()
                if (s.isEmpty()) return emptyList()
                val parsed = com.google.gson.JsonParser.parseString(s)
                if (parsed.isJsonArray) {
                    return parsed.asJsonArray.mapNotNull {
                        try { gson.fromJson(it, ExtraFile::class.java) } catch (_: Exception) { null }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return emptyList()
    }
    fun displayTitle(): String = title?.ifBlank { filename } ?: filename ?: "未命名"
    fun progressRatio(): Float {
        val d = duration ?: 0.0
        val p = progress ?: 0.0
        if (d <= 0 || p <= 0) return 0f
        return (p / d).toFloat().coerceIn(0f, 1f)
    }
    fun parsedCast(): List<String> {
        val raw = castList?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        return try {
            com.google.gson.Gson().fromJson(raw, Array<String>::class.java)?.toList().orEmpty()
        } catch (_: Exception) {
            raw.split(",", "、", ";", "/").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}

data class MediaListResponse(
    val items: List<MediaItem>? = null,
    val total: Int? = null,
    val page: Int? = null,
    @SerializedName("page_size") val pageSize: Int? = null
)

data class LibraryItem(
    val id: Long? = null,
    val name: String? = null,
    val path: String? = null,
    val type: String? = null,
    val count: Int? = null,
    val counts: Int? = null,
    @SerializedName("poster_id") val posterId: Long? = null,
    @SerializedName("scan_status") val scanStatus: String? = null,
    @SerializedName("last_scan") val lastScan: String? = null,
    @SerializedName("item_count") val itemCountField: Int? = null
) {
    fun itemCount(): Int = count ?: counts ?: itemCountField ?: 0
    fun displayName(): String = name?.ifBlank { path } ?: path ?: "媒体库"
}

data class FolderItem(
    val path: String? = null,
    val name: String? = null,
    val folder: String? = null,
    val count: Int? = null
) {
    fun folderPath(): String = path ?: folder ?: name ?: ""
    fun displayName(): String = name ?: path ?: folder ?: ""
}

data class ApiErrorBody(
    val detail: JsonElement? = null
)
