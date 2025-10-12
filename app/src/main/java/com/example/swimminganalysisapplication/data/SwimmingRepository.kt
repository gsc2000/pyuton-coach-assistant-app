package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class SwimmingRepository(private val apiService: ApiService) {

    // メニューリストのインメモリキャッシュ
    private var menusCache: List<Menu>? = null

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

    // --- Auth (OpenAPI仕様準拠) ---
    suspend fun register(userCreate: UserCreate): User? = handleResponse({ apiService.register(userCreate) }, "register")
    suspend fun login(userLogin: UserLogin): Token? = handleResponse({ apiService.login(userLogin) }, "login")
    suspend fun getMe(): User? = handleResponse({ apiService.getMe() }, "getMe")

    // User endpoints
    suspend fun getUser(id: Int): User? = handleResponse({ apiService.getUser(id) }, "getUser")

    // Player endpoints
    suspend fun createPlayer(player: PlayerCreate): Player? = handleResponse({ apiService.createPlayer(player) }, "createPlayer")
    suspend fun getPlayers(query: String? = null): List<Player>? = handleResponse({ apiService.getPlayers(query) }, "getPlayers")
    suspend fun getPlayer(id: Int): Player? = handleResponse({ apiService.getPlayer(id) }, "getPlayer")
    suspend fun updatePlayer(id: Int, player: Player): Player? = handleResponse({ apiService.updatePlayer(id, player) }, "updatePlayer")
    suspend fun deletePlayer(id: Int): Boolean = handleResponse({ apiService.deletePlayer(id) }, "deletePlayer") != null

    // --- Menu Endpoints with Intelligent Caching ---
    suspend fun getMenusByUserId(userId: Int, forceRefresh: Boolean = false): List<Menu>? {
        if (menusCache != null && !forceRefresh) {
            Log.d("SwimmingRepository", "Returning menus from cache.")
            return menusCache
        }
        Log.d("SwimmingRepository", "Fetching menus from network. ForceRefresh: $forceRefresh")
        val menus = handleResponse({ apiService.getMenusByUserId(userId) }, "getMenusByUserId")
        menusCache = menus
        return menusCache
    }

    suspend fun createMenu(menu: Menu): Menu? {
        val createdMenu = handleResponse({ apiService.createMenu(menu) }, "createMenu")
        // 作成成功後、キャッシュに新しいメニューを追加
        if (createdMenu != null) {
            menusCache = menusCache?.plus(createdMenu)
            Log.d("SwimmingRepository", "Added new menu to cache.")
        }
        return createdMenu
    }

    // This method seems unused, but we'll leave it as is.
    suspend fun getMenus(): List<Menu>? = handleResponse({ apiService.getMenus() }, "getMenus")

    // キャッシュから単一のメニューを取得
    suspend fun getMenu(id: Int): Menu? {
        Log.d("SwimmingRepository", "Attempting to get menu with id $id from cache.")
        if (menusCache == null) {
            Log.w("SwimmingRepository", "Menu cache is null. Cannot retrieve menu item.")
            return null
        }
        val menu = menusCache?.find { it.menuId == id }
        if (menu == null) {
            Log.w("SwimmingRepository", "Menu with id $id not found in cache.")
        } else {
            Log.d("SwimmingRepository", "Found menu in cache: $menu")
        }
        return menu
    }

    suspend fun updateMenu(id: Int, menu: Menu): Menu? {
        val updatedMenu = handleResponse({ apiService.updateMenu(id, menu) }, "updateMenu")
        // 更新成功後、キャッシュ内の該当メニューを置き換え
        if (updatedMenu != null) {
            menusCache = menusCache?.map {
                if (it.menuId == id) updatedMenu else it
            }
            Log.d("SwimmingRepository", "Updated menu in cache.")
        }
        return updatedMenu
    }

    suspend fun deleteMenu(id: Int): Boolean {
        val success = handleResponse({ apiService.deleteMenu(id) }, "deleteMenu") != null
        // 削除成功後、キャッシュから該当メニューを削除
        if (success) {
            menusCache = menusCache?.filterNot { it.menuId == id }
            Log.d("SwimmingRepository", "Deleted menu from cache.")
        }
        return success
    }

    // --- Other Endpoints ---

    suspend fun getMenuChats(id: String): List<Chat>? = handleResponse({ apiService.getMenuChats(id) }, "getMenuChats")
    suspend fun getMenuChatThreads(id: String): List<ChatThread>? = handleResponse({ apiService.getMenuChatThreads(id) }, "getMenuChatThreads")
    suspend fun createTag(tag: ApiTag): ApiTag? = handleResponse({ apiService.createTag(tag) }, "createTag")
    suspend fun getTags(): List<ApiTag>? = handleResponse({ apiService.getTags() }, "getTags")
    suspend fun createMenuTagRelation(relation: MenuTagRelation): MenuTagRelation? = handleResponse({ apiService.createMenuTagRelation(relation) }, "createMenuTagRelation")
    suspend fun createChat(chat: Chat): Chat? = handleResponse({ apiService.createChat(chat) }, "createChat")
    suspend fun getChats(): List<Chat>? = handleResponse({ apiService.getChats() }, "getChats")
    suspend fun createFavorite(favorite: Favorite): Favorite? = handleResponse({ apiService.createFavorite(favorite) }, "createFavorite")
    suspend fun getFavorites(): List<Favorite>? = handleResponse({ apiService.getFavorites() }, "getFavorites")
    suspend fun createChatThread(chatThread: ChatThread): ChatThread? = handleResponse({ apiService.createChatThread(chatThread) }, "createChatThread")
    suspend fun getChatThreads(): List<ChatThread>? = handleResponse({ apiService.getChatThreads() }, "getChatThreads")
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
    suspend fun createAttachment(attachment: Attachment): Attachment? = handleResponse({ apiService.createAttachment(attachment) }, "createAttachment")
    suspend fun getAttachments(): List<Attachment>? = handleResponse({ apiService.getAttachments() }, "getAttachments")
}

class ApiException(message: String) : Exception(message)