package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.Analysis
import com.example.swimminganalysisapplication.data.remote.model.Attachment
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.data.remote.model.ChatCreate
import com.example.swimminganalysisapplication.data.remote.model.ChatThread
import com.example.swimminganalysisapplication.data.remote.model.Favorite
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import com.example.swimminganalysisapplication.data.remote.model.Token
import com.example.swimminganalysisapplication.data.remote.model.User
import com.example.swimminganalysisapplication.data.remote.model.UserCreate
import com.example.swimminganalysisapplication.data.remote.model.UserLogin
import com.example.swimminganalysisapplication.data.remote.model.Video
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * API定義 (プロジェクトの現状に合わせた最小限の修正)
 */
interface ApiService {

    @GET
    suspend fun getAnalysisJson(@Url url: String): Response<ResponseBody>

    // --- Auth Endpoints ---
    @POST("/api/v1/auth/register")
    suspend fun register(@Body userCreate: UserCreate): Response<User>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body userLogin: UserLogin): Response<Token>

    @GET("/api/v1/auth/me")
    suspend fun getMe(): Response<User>

    // --- Player Endpoints ---
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

    // --- Menu Endpoints ---
    @GET("/api/v1/menus/user/{user_id}")
    suspend fun getMenusByUserId(@Path("user_id") userId: Int): Response<List<Menu>>

    // POSTは既存の data class 'Menu' を使うように戻す
    @POST("/api/v1/menus/")
    suspend fun createMenu(@Body menu: Menu): Response<Menu>

    // ★★★[新規追加] DiscoverScreen用の公開メニュー取得API ★★★
    @GET("/api/v1/menus/public")
    suspend fun getPublicMenus(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): Response<List<Menu>>

    @GET("/api/v1/menus/{id}")
    suspend fun getMenu(@Path("id") id: Int): Response<Menu>

    // PUTは既存の data class 'Menu' を使うように戻す
    @PUT("/api/v1/menus/{id}")
    suspend fun updateMenu(@Path("id") id: Int, @Body menu: Menu): Response<Menu>

    @DELETE("/api/v1/menus/{id}")
    suspend fun deleteMenu(@Path("id") id: Int): Response<Unit>

    // ★★★[新規追加] DiscoverScreen用のメニューフォークAPI ★★★
    @POST("/api/v1/menus/{public_menu_id}/fork")
    suspend fun forkMenu(@Path("public_menu_id") publicMenuId: Int): Response<Menu>

    // --- Other Endpoints (現状維持) ---
    @POST("/api/v1/chats/")
    suspend fun createChat(@Body chat: ChatCreate): Response<Chat>

    @GET("/api/v1/chats/")
    suspend fun getChats(): Response<List<Chat>>

    @POST("favorites/")
    suspend fun createFavorite(@Body favorite: Favorite): Response<Favorite>

    @GET("favorites/")
    suspend fun getFavorites(): Response<List<Favorite>>

    @POST("videos/")
    suspend fun createVideo(@Body video: Video): Response<Video>

    @GET("videos/")
    suspend fun getVideos(): Response<List<Video>>

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

    @POST("attachments/")
    suspend fun createAttachment(@Body attachment: Attachment): Response<Attachment>

    @GET("attachments/")
    suspend fun getAttachments(): Response<List<Attachment>>

    // 未使用またはモデルが存在しないものはコメントアウト
    //    @GET("menus/{id}/chats")
    //    suspend fun getMenuChats(@Path("id") id: String): Response<List<Chat>>
    //
    //    @GET("menus/{id}/chat_threads")
    //    suspend fun getMenuChatThreads(@Path("id") id: String): Response<List<ChatThread>>
    //
    //    @POST("tags/")
    //    suspend fun createTag(@Body tag: ApiTag): Response<ApiTag>
    //
    //    @GET("tags/")
    //    suspend fun getTags(): Response<List<ApiTag>>
    //
    //    @POST("menu_tag_relations/")
    //    suspend fun createMenuTagRelation(@Body relation: MenuTagRelation): Response<MenuTagRelation>
    //
    //    @POST("chat_threads/")
    //    suspend fun createChatThread(@Body chatThread: ChatThread): Response<ChatThread>
    //
    //    @GET("chat_threads/")
    //    suspend fun getChatThreads(): Response<List<ChatThread>>
}