package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

class SwimmingRepository(private val apiService: ApiService) {

    private val menuCache = mutableMapOf<Int, Menu>()

    private suspend fun <T> handleResponse(
        apiCall: suspend () -> Response<T>,
        operation: String
    ): T? {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                Log.i("SwimmingRepository", "$operation successful.")
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

    // --- Auth ---
    suspend fun register(userCreate: UserCreate): User? = handleResponse({ apiService.register(userCreate) }, "register")
    suspend fun login(userLogin: UserLogin): Token? = handleResponse({ apiService.login(userLogin) }, "login")
    suspend fun getMe(): User? = handleResponse({ apiService.getMe() }, "getMe")

    // --- Player endpoints ---
    suspend fun createPlayer(player: PlayerCreate): Player? = handleResponse({ apiService.createPlayer(player) }, "createPlayer")
    suspend fun getPlayers(query: String? = null): List<Player>? = handleResponse({ apiService.getPlayers(query) }, "getPlayers")
    suspend fun getPlayer(id: Int): Player? = handleResponse({ apiService.getPlayer(id) }, "getPlayer")
    suspend fun updatePlayer(id: Int, player: Player): Player? = handleResponse({ apiService.updatePlayer(id, player) }, "updatePlayer")
    suspend fun deletePlayer(id: Int): Boolean = handleResponse({ apiService.deletePlayer(id) }, "deletePlayer") != null

    // --- Menu Endpoints ---
    suspend fun getMenusByUserId(userId: Int, forceRefresh: Boolean = false): List<Menu>? {
        val menus = handleResponse({ apiService.getMenusByUserId(userId) }, "getMenusByUserId")
        menus?.forEach { menuCache[it.menuId] = it }
        return menus
    }

    suspend fun createMenu(menu: Menu): Menu? {
        val createdMenu = handleResponse({ apiService.createMenu(menu) }, "createMenu")
        createdMenu?.let { menuCache[it.menuId] = it }
        return createdMenu
    }

    suspend fun getPublicMenus(): List<Menu>? {
        val publicMenus = handleResponse({ apiService.getPublicMenus() }, "getPublicMenus")
        publicMenus?.forEach { menuCache[it.menuId] = it }
        return publicMenus
    }

    suspend fun getMenu(id: Int): Menu? {
        Log.d("SwimmingRepository", "Attempting to get menu with id $id from cache.")
        val cachedMenu = menuCache[id]
        if (cachedMenu == null) {
            Log.w("SwimmingRepository", "Menu with id $id not found in cache.")
        }
        return cachedMenu
    }

    suspend fun updateMenu(id: Int, menu: Menu): Menu? {
        val updatedMenu = handleResponse({ apiService.updateMenu(id, menu) }, "updateMenu")
        updatedMenu?.let { menuCache[it.menuId] = it }
        return updatedMenu
    }

    suspend fun deleteMenu(id: Int): Boolean {
        val success = handleResponse({ apiService.deleteMenu(id) }, "deleteMenu") != null
        if (success) {
            menuCache.remove(id)
        }
        return success
    }

    suspend fun forkMenu(menuId: Int): Menu? {
        val forkedMenu = handleResponse({ apiService.forkMenu(menuId) }, "forkMenu")
        forkedMenu?.let { menuCache[it.menuId] = it }
        return forkedMenu
    }

    suspend fun uploadVideo(videoFile: MultipartBody.Part): VideoUploadResponse? {
        return handleResponse({ apiService.uploadVideo(videoFile) }, "uploadVideo")
    }

    // ★★★ ここから追加 ★★★
    suspend fun startInferenceJob(jobRequest: JobRequest): JobResponse? {
        return handleResponse({ apiService.inferenceJob(jobRequest) }, "startInferenceJob")
    }
    // ★★★ ここまで追加 ★★★

    // --- Other Endpoints ---
    suspend fun getMenuChats(menuId: String): List<Chat>? {
        val menuIdInt = menuId.toIntOrNull() ?: return emptyList()
        val allChats = handleResponse({ apiService.getChats() }, "getChats")
        return allChats?.filter { it.menuId == menuIdInt }
    }
    suspend fun createChat(chat: ChatCreate): Chat? = handleResponse({ apiService.createChat(chat) }, "createChat")
    suspend fun getChats(): List<Chat>? = handleResponse({ apiService.getChats() }, "getChats")
    suspend fun createFavorite(favorite: Favorite): Favorite? = handleResponse({ apiService.createFavorite(favorite) }, "createFavorite")
    suspend fun getFavorites(): List<Favorite>? = handleResponse({ apiService.getFavorites() }, "getFavorites")
    suspend fun createVideo(video: Video): Video? = handleResponse({ apiService.createVideo(video) }, "createVideo")
    suspend fun getVideos(): List<Video>? = handleResponse({ apiService.getVideos() }, "getVideos")
    suspend fun createAnalysis(analysis: Analysis): Analysis? = handleResponse({ apiService.createAnalysis(analysis) }, "createAnalysis")
    suspend fun getAnalyses(): List<Analysis>? = handleResponse({ apiService.getAnalyses() }, "getAnalyses")
    suspend fun getAnalysis(id: Int): Analysis? = handleResponse({ apiService.getAnalysis(id) }, "getAnalysis")
    suspend fun uploadSingleAnalysis(
        date: RequestBody,
        playerId: RequestBody,
        comment: RequestBody,
        video: MultipartBody.Part
    ): Analysis? = handleResponse({ apiService.uploadSingleAnalysis(date, playerId, comment, video) }, "uploadSingleAnalysis")

    suspend fun getAnalysisJson(url: String): String? {
        return try {
            val response = apiService.getAnalysisJson(url)
            if (response.isSuccessful) {
                response.body()?.string()
            } else {
                throw ApiException("分析JSONの取得に失敗しました: ${response.code()}")
            }
        } catch (e: Exception) {
            throw ApiException("分析JSONの取得中に例外が発生しました: ${e.message}")
        }
    }

    suspend fun createAttachment(attachment: Attachment): Attachment? = handleResponse({ apiService.createAttachment(attachment) }, "createAttachment")
    suspend fun getAttachments(): List<Attachment>? = handleResponse({ apiService.getAttachments() }, "getAttachments")
}

class ApiException(message: String) : Exception(message)