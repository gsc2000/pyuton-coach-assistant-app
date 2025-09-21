package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class SwimmingRepository(private val apiService: ApiService) {

    private suspend fun <T> handleResponse(
        apiCall: suspend () -> Response<T>,
        successMessage: String,
        operation: String
    ): T? {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                Log.i("SwimmingRepository", "$successMessage - $operation successful.")
                return response.body()
            } else {
                val errorMsg = "Failed to $operation: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}"
                Log.e("SwimmingRepository", errorMsg)
                throw ApiException("$operation に失敗しました: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SwimmingRepository", "Exception during $operation: ${e.message}", e)
            throw ApiException("$operation 中に例外が発生しました: ${e.message}")
        }
    }

    // --- User ---
    suspend fun createUser(user: User): User? = handleResponse({ apiService.createUser(user) }, "Created user", "createUser")
    suspend fun getUser(id: Int): User? = handleResponse({ apiService.getUser(id) }, "Fetched user", "getUser")
    suspend fun login(credentials: Map<String, String>): Response<Token> = apiService.login(credentials)
    suspend fun getMe(): User? = handleResponse({ apiService.getMe() }, "Fetched self user", "getMe")


    // --- Player ---
    suspend fun createPlayer(player: PlayerCreate): Player? = handleResponse({ apiService.createPlayer(player) }, "Created player", "createPlayer")
    suspend fun getPlayers(query: String? = null): List<Player>? = handleResponse({ apiService.getPlayers(query) }, "Fetched players", "getPlayers")
    suspend fun getPlayer(id: Int): Player? = handleResponse({ apiService.getPlayer(id) }, "Fetched player", "getPlayer")
    suspend fun updatePlayer(id: Int, player: Player): Player? = handleResponse({ apiService.updatePlayer(id, player) }, "Updated player", "updatePlayer")
    suspend fun deletePlayer(id: Int): Boolean = handleResponse({ apiService.deletePlayer(id) }, "Deleted player", "deletePlayer") != null

    // --- Menu ---
    suspend fun createMenu(menu: Menu): Menu? = handleResponse({ apiService.createMenu(menu) }, "Created menu", "createMenu")
    suspend fun getMenus(): List<Menu>? = handleResponse({ apiService.getMenus() }, "Fetched menus", "getMenus")
    suspend fun getMenu(id: Int): Menu? = handleResponse({ apiService.getMenu(id) }, "Fetched menu", "getMenu")
    suspend fun updateMenu(id: Int, menu: Menu): Menu? = handleResponse({ apiService.updateMenu(id, menu) }, "Updated menu", "updateMenu")
    suspend fun deleteMenu(id: Int): Boolean = handleResponse({ apiService.deleteMenu(id) }, "Deleted menu", "deleteMenu") != null

    // --- Tag ---
    suspend fun createTag(tag: ApiTag): ApiTag? = handleResponse({ apiService.createTag(tag) }, "Created tag", "createTag")
    suspend fun getTags(): List<ApiTag>? = handleResponse({ apiService.getTags() }, "Fetched tags", "getTags")

    // --- MenuTagRelation ---
    suspend fun createMenuTagRelation(relation: MenuTagRelation): MenuTagRelation? = handleResponse({ apiService.createMenuTagRelation(relation) }, "Created menu tag relation", "createMenuTagRelation")

    // --- Chat ---
    suspend fun createChat(chat: Chat): Chat? = handleResponse({ apiService.createChat(chat) }, "Created chat", "createChat")
    suspend fun getChats(): List<Chat>? = handleResponse({ apiService.getChats() }, "Fetched chats", "getChats")

    // --- Favorite ---
    suspend fun createFavorite(favorite: Favorite): Favorite? = handleResponse({ apiService.createFavorite(favorite) }, "Created favorite", "createFavorite")
    suspend fun getFavorites(): List<Favorite>? = handleResponse({ apiService.getFavorites() }, "Fetched favorites", "getFavorites")

    // --- ChatThread ---
    suspend fun createChatThread(chatThread: ChatThread): ChatThread? = handleResponse({ apiService.createChatThread(chatThread) }, "Created chat thread", "createChatThread")
    suspend fun getChatThreads(): List<ChatThread>? = handleResponse({ apiService.getChatThreads() }, "Fetched chat threads", "getChatThreads")

    // --- Video ---
    suspend fun createVideo(video: Video): Video? = handleResponse({ apiService.createVideo(video) }, "Created video", "createVideo")
    suspend fun getVideos(): List<Video>? = handleResponse({ apiService.getVideos() }, "Fetched videos", "getVideos")

    // --- Analysis ---
    suspend fun createAnalysis(analysis: Analysis): Analysis? = handleResponse({ apiService.createAnalysis(analysis) }, "Created analysis", "createAnalysis")
    suspend fun getAnalyses(): List<Analysis>? = handleResponse({ apiService.getAnalyses() }, "Fetched analyses", "getAnalyses")
    suspend fun getAnalysis(id: Int): Analysis? = handleResponse({ apiService.getAnalysis(id) }, "Fetched analysis", "getAnalysis")
    suspend fun uploadSingleAnalysis(
        date: RequestBody,
        playerId: RequestBody,
        comment: RequestBody,
        video: MultipartBody.Part
    ): Analysis? = handleResponse({ apiService.uploadSingleAnalysis(date, playerId, comment, video) }, "Uploaded single analysis", "uploadSingleAnalysis")

    suspend fun getAnalysisJson(url: String): String? {
        return try {
            val response = apiService.getAnalysisJson(url)
            if (response.isSuccessful) {
                Log.i("SwimmingRepository", "Fetched analysis JSON successfully.")
                response.body()?.string()
            } else {
                val errorMsg = "Failed to fetch analysis JSON: ${response.code()} ${response.message()}"
                Log.e("SwimmingRepository", errorMsg)
                throw ApiException("分析JSONの取得に失敗しました: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("SwimmingRepository", "Exception during getAnalysisJson: ${e.message}", e)
            throw ApiException("分析JSONの取得中に例外が発生しました: ${e.message}")
        }
    }

    // --- Attachment ---
    suspend fun createAttachment(attachment: Attachment): Attachment? = handleResponse({ apiService.createAttachment(attachment) }, "Created attachment", "createAttachment")
    suspend fun getAttachments(): List<Attachment>? = handleResponse({ apiService.getAttachments() }, "Fetched attachments", "getAttachments")
}

class ApiException(message: String) : Exception(message)
