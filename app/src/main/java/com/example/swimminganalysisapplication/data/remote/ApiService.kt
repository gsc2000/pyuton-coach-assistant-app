package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET
    suspend fun getAnalysisJson(@Url url: String): Response<ResponseBody>

    // --- Auth Endpoints (OpenAPI仕様準拠) ---
    @POST("/api/v1/auth/register")
    suspend fun register(@Body userCreate: UserCreate): Response<User>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body userLogin: UserLogin): Response<Token>

    @GET("/api/v1/auth/me")
    suspend fun getMe(): Response<User>


    // --- ここから下の古いエンドポイントは、今後のエラー発生時に順次修正します ---

    // User endpoints
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>

    // Player endpoints
    @POST("players/")
    suspend fun createPlayer(@Body player: PlayerCreate): Response<Player>

    @GET("players/")
    suspend fun getPlayers(@Query("q") query: String?): Response<List<Player>>

    @GET("players/{id}")
    suspend fun getPlayer(@Path("id") id: Int): Response<Player>

    @PUT("players/{id}")
    suspend fun updatePlayer(@Path("id") id: Int, @Body player: Player): Response<Player>

    @DELETE("players/{id}")
    suspend fun deletePlayer(@Path("id") id: Int): Response<Unit>

    // Menu endpoints
    // ★★★ ここから追記 ★★★
    @GET("/api/v1/menus/user/{user_id}")
    suspend fun getMenusByUserId(@Path("user_id") userId: Int): Response<List<Menu>>
    // ★★★ ここまで追記 ★★★

    @POST("menus/")
    suspend fun createMenu(@Body menu: Menu): Response<Menu>

    @GET("menus/")
    suspend fun getMenus(): Response<List<Menu>>

    @GET("menus/{id}")
    suspend fun getMenu(@Path("id") id: Int): Response<Menu>

    @PUT("menus/{id}")
    suspend fun updateMenu(@Path("id") id: Int, @Body menu: Menu): Response<Menu>

    @DELETE("menus/{id}")
    suspend fun deleteMenu(@Path("id") id: Int): Response<Unit>

    @GET("menus/{id}/chats")
    suspend fun getMenuChats(@Path("id") id: String): Response<List<Chat>>

    @GET("menus/{id}/chat_threads")
    suspend fun getMenuChatThreads(@Path("id") id: String): Response<List<ChatThread>>

    // Tag endpoints
    @POST("tags/")
    suspend fun createTag(@Body tag: ApiTag): Response<ApiTag>

    @GET("tags/")
    suspend fun getTags(): Response<List<ApiTag>>

    // MenuTagRelation endpoints
    @POST("menu_tag_relations/")
    suspend fun createMenuTagRelation(@Body relation: MenuTagRelation): Response<MenuTagRelation>

    // Chat endpoints
    @POST("chats/")
    suspend fun createChat(@Body chat: Chat): Response<Chat>

    @GET("chats/")
    suspend fun getChats(): Response<List<Chat>>

    // Favorite endpoints
    @POST("favorites/")
    suspend fun createFavorite(@Body favorite: Favorite): Response<Favorite>

    @GET("favorites/")
    suspend fun getFavorites(): Response<List<Favorite>>

    // ChatThread endpoints
    @POST("chat_threads/")
    suspend fun createChatThread(@Body chatThread: ChatThread): Response<ChatThread>

    @GET("chat_threads/")
    suspend fun getChatThreads(): Response<List<ChatThread>>

    // Video endpoints
    @POST("videos/")
    suspend fun createVideo(@Body video: Video): Response<Video>

    @GET("videos/")
    suspend fun getVideos(): Response<List<Video>>

    // Analysis endpoints
    @POST("analyses/")
    suspend fun createAnalysis(@Body analysis: Analysis): Response<Analysis>

    @GET("analyses/")
    suspend fun getAnalyses(): Response<List<Analysis>>

    @GET("analyses/{id}")
    suspend fun getAnalysis(@Path("id") id: Int): Response<Analysis>

    @Multipart
    @POST("analyses/single")
    suspend fun uploadSingleAnalysis(
        @Part("date") date: RequestBody,
        @Part("player_id") playerId: RequestBody,
        @Part("comment") comment: RequestBody,
        @Part video: MultipartBody.Part
    ): Response<Analysis>

    // Attachment endpoints
    @POST("attachments/")
    suspend fun createAttachment(@Body attachment: Attachment): Response<Attachment>

    @GET("attachments/")
    suspend fun getAttachments(): Response<List<Attachment>>
}