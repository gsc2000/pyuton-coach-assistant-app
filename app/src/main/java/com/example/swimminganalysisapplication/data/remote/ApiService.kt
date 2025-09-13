package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.Comment
import com.example.swimminganalysisapplication.data.remote.model.Favorite
import com.example.swimminganalysisapplication.data.remote.model.PracticeMenu
import com.example.swimminganalysisapplication.data.remote.model.User
import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface ApiService {

    // 2.1 認証 (/auth)
    @POST("auth/register")
    suspend fun register(@Body user: Map<String, String>): Response<User>

    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): Response<Map<String, String>> // e.g., {"token": "..."}

    @GET("auth/me")
    suspend fun getMe(): Response<User>

    // 2.2 練習メニュー (/menus)
    @POST("menus")
    suspend fun createMenu(@Body practiceMenu: PracticeMenu): Response<PracticeMenu>

    @GET("menus")
    suspend fun getMyMenus(): Response<List<PracticeMenu>>

    @GET("menus/public")
    suspend fun getPublicMenus(
        @Query("search") query: String?,
        @Query("tags") tags: String?, // Comma-separated
        @Query("page") page: Int?,
        @Query("limit") limit: Int?
    ): Response<List<PracticeMenu>>

    @GET("menus/{menu_id}")
    suspend fun getMenuById(@Path("menu_id") menuId: UUID): Response<PracticeMenu>

    @PUT("menus/{menu_id}")
    suspend fun updateMenu(@Path("menu_id") menuId: UUID, @Body practiceMenu: PracticeMenu): Response<PracticeMenu>

    @DELETE("menus/{menu_id}")
    suspend fun deleteMenu(@Path("menu_id") menuId: UUID): Response<Unit>

    @POST("menus/{public_menu_id}/fork")
    suspend fun forkMenu(@Path("public_menu_id") publicMenuId: UUID): Response<PracticeMenu>

    @GET("menus/{menu_id}/check_update")
    suspend fun checkUpdate(@Path("menu_id") menuId: UUID): Response<Map<String, Boolean>> // e.g., {"has_update": true}

    @POST("menus/{menu_id}/pull_update")
    suspend fun pullUpdate(@Path("menu_id") menuId: UUID): Response<PracticeMenu>

    // 2.3 コメント (/menus/{menu_id}/comments)
    @POST("menus/{menu_id}/comments")
    suspend fun postComment(@Path("menu_id") menuId: UUID, @Body comment: Map<String, String>): Response<Comment>

    @GET("menus/{menu_id}/comments")
    suspend fun getComments(@Path("menu_id") menuId: UUID): Response<List<Comment>>

    @DELETE("comments/{comment_id}")
    suspend fun deleteComment(@Path("comment_id") commentId: UUID): Response<Unit>

    // 2.4 お気に入り (/users/me/favorites)
    @POST("users/me/favorites/{public_menu_id}")
    suspend fun addFavorite(@Path("public_menu_id") publicMenuId: UUID): Response<Favorite>

    @DELETE("users/me/favorites/{public_menu_id}")
    suspend fun removeFavorite(@Path("public_menu_id") publicMenuId: UUID): Response<Unit>

    @GET("users/me/favorites")
    suspend fun getFavorites(): Response<List<Favorite>>
}

