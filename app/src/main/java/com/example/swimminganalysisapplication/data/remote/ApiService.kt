package com.example.swimminganalysisapplication.data.remote

import com.example.swimminganalysisapplication.data.remote.model.Analysis
import com.example.swimminganalysisapplication.data.remote.model.Attachment
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.data.remote.model.ChatCreate
import com.example.swimminganalysisapplication.data.remote.model.ChatThread
import com.example.swimminganalysisapplication.data.remote.model.Favorite
import com.example.swimminganalysisapplication.data.remote.model.JobRequest
import com.example.swimminganalysisapplication.data.remote.model.JobResponse
import com.example.swimminganalysisapplication.data.remote.model.JobStatus
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import com.example.swimminganalysisapplication.data.remote.model.Token
import com.example.swimminganalysisapplication.data.remote.model.User
import com.example.swimminganalysisapplication.data.remote.model.UserCreate
import com.example.swimminganalysisapplication.data.remote.model.UserLogin
import com.example.swimminganalysisapplication.data.remote.model.Video
import com.example.swimminganalysisapplication.data.remote.model.VideoCreate
import com.example.swimminganalysisapplication.data.remote.model.VideoUploadResponse
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
import retrofit2.http.Streaming
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

    // ★★★ Player Endpoints のパスを修正 ★★★
    @POST("/api/v1/players/")
    suspend fun createPlayer(@Body player: PlayerCreate): Response<Player>

    @GET("/api/v1/players/")
    suspend fun getPlayers(@Query("q") query: String?): Response<List<Player>>

    @GET("/api/v1/players/{id}")
    suspend fun getPlayer(@Path("id") id: Int): Response<Player>

    @PUT("/api/v1/players/{id}")
    suspend fun updatePlayer(@Path("id") id: Int, @Body player: Player): Response<Player>

    @DELETE("/api/v1/players/{id}")
    suspend fun deletePlayer(@Path("id") id: Int): Response<Unit>

    // --- Menu Endpoints ---
    @GET("/api/v1/menus/user/{user_id}")
    suspend fun getMenusByUserId(@Path("user_id") userId: Int): Response<List<Menu>>

    @POST("/api/v1/menus/")
    suspend fun createMenu(@Body menu: Menu): Response<Menu>

    @GET("/api/v1/menus/public")
    suspend fun getPublicMenus(@Query("skip") skip: Int = 0, @Query("limit") limit: Int = 100): Response<List<Menu>>

    @GET("/api/v1/menus/{id}")
    suspend fun getMenu(@Path("id") id: Int): Response<Menu>

    @PUT("/api/v1/menus/{id}")
    suspend fun updateMenu(@Path("id") id: Int, @Body menu: Menu): Response<Menu>

    @DELETE("/api/v1/menus/{id}")
    suspend fun deleteMenu(@Path("id") id: Int): Response<Unit>

    @POST("/api/v1/menus/{public_menu_id}/fork")
    suspend fun forkMenu(@Path("public_menu_id") publicMenuId: Int): Response<Menu>

    // --- Upload Endpoint ---
    @Multipart
    @POST("/api/v1/upload/video")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part
    ): Response<VideoUploadResponse>

    // --- Job Endpoint ---
    @POST("/api/v1/job/inference")
    suspend fun inferenceJob(@Body jobRequest: JobRequest): Response<JobResponse>

    @GET("/api/v1/job/status/{job_id}")
    suspend fun getJobStatus(@Path("job_id") jobId: String): Response<JobStatus>

    // --- Download Endpoint ---
    @Streaming
    @GET("/api/v1/download/result_video/{video_uuid}")
    suspend fun downloadResultVideo(@Path("video_uuid") videoUuid: String): Response<ResponseBody>

    // --- Other Endpoints (パスを修正) ---
    @POST("/api/v1/chats/")
    suspend fun createChat(@Body chat: ChatCreate): Response<Chat>

    @GET("/api/v1/chats/")
    suspend fun getChats(): Response<List<Chat>>

    @POST("/api/v1/favorites/")
    suspend fun createFavorite(@Body favorite: Favorite): Response<Favorite>

    @GET("/api/v1/favorites/")
    suspend fun getFavorites(): Response<List<Favorite>>

    @POST("/api/v1/videos/")
    suspend fun createVideo(@Body video: VideoCreate): Response<Video>

    @GET("/api/v1/videos/")
    suspend fun getVideos(): Response<List<Video>>

    @POST("/api/v1/analyses/")
    suspend fun createAnalysis(@Body analysis: Analysis): Response<Analysis>

    @GET("/api/v1/analyses/")
    suspend fun getAnalyses(): Response<List<Analysis>>

    @GET("/api/v1/analyses/{id}")
    suspend fun getAnalysis(@Path("id") id: Int): Response<Analysis>

    @Multipart
    @POST("/api/v1/analyses/single")
    suspend fun uploadSingleAnalysis(
        @Part("date") date: RequestBody,
        @Part("player_id") playerId: RequestBody,
        @Part("comment") comment: RequestBody,
        @Part video: MultipartBody.Part
    ): Response<Analysis>

    @POST("/api/v1/attachments/")
    suspend fun createAttachment(@Body attachment: Attachment): Response<Attachment>

    @GET("/api/v1/attachments/")
    suspend fun getAttachments(): Response<List<Attachment>>
}
