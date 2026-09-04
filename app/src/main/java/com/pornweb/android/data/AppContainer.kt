package com.pornweb.android.data

import android.content.Context
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val serverStore = ServerStore(context)
    val tokenStore = TokenStore(context)
    val playerPrefs = PlayerPrefs(context)
    val gson: Gson = GsonBuilder().serializeNulls().create()

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized = _unauthorized.asSharedFlow()

    private val baseUrlInterceptor = Interceptor { chain ->
        val orig = chain.request()
        val base = serverStore.normalizedBase().toHttpUrlOrNull()
            ?: return@Interceptor chain.proceed(orig)
        val joinedPath = (base.encodedPath.trimEnd('/') + orig.url.encodedPath).let {
            if (it.startsWith("/")) it else "/$it"
        }.replace("//", "/")
        val newUrl = orig.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(if (joinedPath.startsWith("/")) joinedPath else "/$joinedPath")
            .build()
        chain.proceed(orig.newBuilder().url(newUrl).build())
    }

    private val authInterceptor = Interceptor { chain ->
        val orig = chain.request()
        val token = tokenStore.token
        val builder = orig.newBuilder()
            .header("Accept", "application/json")
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        val resp = chain.proceed(builder.build())
        if (resp.code == 401) {
            val path = orig.url.encodedPath
            if (!path.contains("/auth/login") && !path.contains("/auth/register")) {
                tokenStore.clear()
                _unauthorized.tryEmit(Unit)
            }
        }
        resp
    }

    private fun logging(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { msg -> Log.d("PornWebHttp", msg) }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    val streamClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl("http://127.0.0.1/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    // Images must not inherit Accept: application/json from the API client.
    private val imageHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = tokenStore.token
            val b = chain.request().newBuilder()
                .header("Accept", "image/*,*/*;q=0.8")
            if (!token.isNullOrBlank()) {
                b.header("Authorization", "Bearer $token")
            }
            chain.proceed(b.build())
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(imageHttpClient)
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()

    fun posterUrl(id: Long?): String {
        if (id == null || id <= 0) return ""
        return mediaAssetUrl("poster", id)
    }

    fun fanartUrl(id: Long?): String {
        if (id == null || id <= 0) return ""
        return mediaAssetUrl("fanart", id)
    }

    fun streamUrl(id: Long, part: Int): String {
        val token = Uri.encode(tokenStore.token.orEmpty())
        val base = serverStore.normalizedBase()
        return "$base/api/media/stream/$id?token=$token&part=$part"
    }

    fun resolveImage(item: MediaItem?, kind: String = "poster"): String {
        if (item == null) return ""
        val id = item.mediaId()
        if (id <= 0L) return ""
        val raw = if (kind == "fanart") item.fanartUrl else item.posterUrl
        // Absolute remote URLs: keep them. Relative /media/... paths 404 on nginx —
        // always use the authenticated API endpoints instead.
        if (!raw.isNullOrBlank() && (raw.startsWith("http://") || raw.startsWith("https://"))) {
            return appendToken(raw)
        }
        return if (kind == "fanart") {
            // Fanart is optional; fall back to poster if missing.
            if (raw.isNullOrBlank()) posterUrl(id) else fanartUrl(id)
        } else {
            posterUrl(id)
        }
    }

    private fun mediaAssetUrl(kind: String, id: Long): String {
        val token = Uri.encode(tokenStore.token.orEmpty())
        val base = serverStore.normalizedBase()
        return "$base/api/media/$kind/$id?token=$token"
    }

    private fun appendToken(url: String): String {
        val token = tokenStore.token.orEmpty()
        if (token.isBlank()) return url
        val sep = if (url.contains("?")) "&" else "?"
        return if (url.contains("token=")) url else "$url${sep}token=${Uri.encode(token)}"
    }

    fun parseError(e: Throwable): String {
        if (e is HttpException) {
            val raw = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            if (!raw.isNullOrBlank()) {
                try {
                    val body = gson.fromJson(raw, ApiErrorBody::class.java)
                    val d = body.detail
                    if (d != null) {
                        if (d.isJsonPrimitive) return d.asString
                        if (d.isJsonArray && d.asJsonArray.size() > 0) {
                            val first = d.asJsonArray[0]
                            if (first.isJsonObject && first.asJsonObject.has("msg")) {
                                return first.asJsonObject.get("msg").asString
                            }
                            return first.toString()
                        }
                    }
                } catch (_: Exception) {
                    return raw.take(200)
                }
            }
            if (e.code() == 401) return "登录已过期，请重新登录"
            return "请求失败 (${e.code()})"
        }
        return e.message?.ifBlank { "网络错误" } ?: "网络错误"
    }

    fun parseStringList(el: JsonElement?): List<String> {
        if (el == null || el.isJsonNull) return emptyList()
        if (el.isJsonArray) {
            return el.asJsonArray.mapNotNull { item ->
                when {
                    item.isJsonPrimitive -> item.asString
                    item.isJsonObject -> item.asJsonObject.get("name")?.asString
                        ?: item.asJsonObject.get("genre")?.asString
                    else -> null
                }
            }.filter { it.isNotBlank() }
        }
        return emptyList()
    }

    fun parseLibraries(el: JsonElement?): List<LibraryItem> {
        if (el == null || el.isJsonNull) return emptyList()
        val arr = when {
            el.isJsonArray -> el.asJsonArray
            el.isJsonObject && el.asJsonObject.has("items") ->
                el.asJsonObject.get("items").asJsonArray
            else -> return emptyList()
        }
        return arr.mapNotNull {
            try {
                gson.fromJson(it, LibraryItem::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun parseFolders(el: JsonElement?): List<FolderItem> {
        if (el == null || el.isJsonNull) return emptyList()
        if (!el.isJsonArray) return emptyList()
        return el.asJsonArray.mapNotNull { item ->
            try {
                if (item.isJsonPrimitive) {
                    FolderItem(path = item.asString, name = item.asString)
                } else {
                    gson.fromJson(item, FolderItem::class.java)
                }
            } catch (_: Exception) {
                null
            }
        }.filter { it.folderPath().isNotBlank() }
    }
}
