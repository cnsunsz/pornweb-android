package com.pornweb.android.data

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(): User

    @GET("api/media/list")
    suspend fun mediaList(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null,
        @Query("genre") genre: String? = null,
        @Query("folder") folder: String? = null,
        @Query("sort") sort: String? = "newest"
    ): MediaListResponse

    @GET("api/media/detail/{id}")
    suspend fun detail(@Path("id") id: Long): MediaItem

    @GET("api/media/continue")
    suspend fun continueWatching(): MediaListResponse

    @PUT("api/media/progress/{id}")
    suspend fun saveProgress(@Path("id") id: Long, @Body body: ProgressRequest): Response<Unit>

    @GET("api/media/genres")
    suspend fun genres(): JsonElement

    @GET("api/media/folders")
    suspend fun folders(): JsonElement

    @GET("api/libraries/")
    suspend fun libraries(): JsonElement


    @GET("api/actors")
    suspend fun actors(@Query("search") search: String? = null): ActorListResponse

    @GET("api/actors/{name}/media")
    suspend fun actorMedia(
        @Path("name") name: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort") sort: String? = "newest"
    ): MediaListResponse

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body body: PasswordChangeRequest): Response<Unit>
}
