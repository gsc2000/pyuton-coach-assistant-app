package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET
    suspend fun getAnalysisJson(@Url url: String): Response<ResponseBody>

    // User endpoints
    @POST("users/")
    suspend fun createUser(@Body user: User): Response<User>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): Response<User>

    @POST("token")
    suspend fun login(@Body credentials: Map<String, String>): Response<Token>

    @GET("users/me/")
    suspend fun getMe(): Response<User>


    // Player endpoints
    @POST("players/")
    suspend fun createPlayer(@Body player: Player): Response<Player>

    @GET("players/")
    suspend fun getPlayers(): Response<List<Player>>

    @GET("players/{id}")
    suspend fun getPlayer(@Path("id") id: Int): Response<Player>

    @PUT("players/{id}")
    suspend fun updatePlayer(@Path("id") id: Int, @Body player: Player): Response<Player>

    @DELETE("players/{id}")
    suspend fun deletePlayer(@Path("id") id: Int): Response<Unit>

    // Menu endpoints
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

    // Attachment endpoints
    @POST("attachments/")
    suspend fun createAttachment(@Body attachment: Attachment): Response<Attachment>

    @GET("attachments/")
    suspend fun getAttachments(): Response<List<Attachment>>
}

