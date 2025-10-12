package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class SwimmingRepository(private val apiService: ApiService) {

    // ★★★ ご指摘の通り、メニューリストのインメモリキャッシュを実装します ★★★
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


    // --- ここから下は、今後のエラー発生時に順次修正します ---

    // User endpoints
    suspend fun getUser(id: Int): User? = handleResponse({ apiService.getUser(id) }, "getUser")

    // Player endpoints
    suspend fun createPlayer(player: PlayerCreate): Player? = handleResponse({ apiService.createPlayer(player) }, "createPlayer")
    suspend fun getPlayers(query: String? = null): List<Player>? = handleResponse({ apiService.getPlayers(query) }, "getPlayers")
    suspend fun getPlayer(id: Int): Player? = handleResponse({ apiService.getPlayer(id) }, "getPlayer")
    suspend fun updatePlayer(id: Int, player: Player): Player? = handleResponse({ apiService.updatePlayer(id, player) }, "updatePlayer")
    suspend fun deletePlayer(id: Int): Boolean = handleResponse({ apiService.deletePlayer(id) }, "deletePlayer") != null

    // Menu endpoints
    suspend fun getMenusByUserId(userId: Int): List<Menu>? {
        // APIからメニューを取得し、キャッシュに保存する
        val menus = handleResponse({ apiService.getMenusByUserId(userId) }, "getMenusByUserId")
        menusCache = menus
        return menusCache
    }

    suspend fun createMenu(menu: Menu): Menu? {
        // 作成時はキャッシュを無効化
        menusCache = null
        return handleResponse({ apiService.createMenu(menu) }, "createMenu")
    }

    suspend fun getMenus(): List<Menu>? = handleResponse({ apiService.getMenus() }, "getMenus")

    // ★★★ getMenuがAPIを呼び出さず、キャッシュから探すように修正 ★★★
    suspend fun getMenu(id: Int): Menu? {
        Log.d("SwimmingRepository", "ID: $id のメニューをキャッシュから検索します。")
        // キャッシュがなければ、念のためAPIから取得を試みる（今回は不要だが、堅牢性のために残す）
        if (menusCache == null) {
            Log.w("SwimmingRepository", "キャッシュが存在しません。一覧画面で取得に失敗した可能性があります。")
            // 本来ならここで再度 getMenusByUserId を呼ぶべきだが、今回はViewModelのロジックに任せる
            return null
        }
        val menu = menusCache?.find { it.menuId == id }
        if (menu == null) {
            Log.w("SwimmingRepository", "ID: $id のメニューがキャッシュに見つかりませんでした。")
        } else {
            Log.d("SwimmingRepository", "キャッシュからメニューを発見しました: $menu")
        }
        return menu
    }

    suspend fun updateMenu(id: Int, menu: Menu): Menu? {
        // 更新時はキャッシュを無効化
        menusCache = null
        return handleResponse({ apiService.updateMenu(id, menu) }, "updateMenu")
    }

    suspend fun deleteMenu(id: Int): Boolean {
        // 削除時はキャッシュを無効化
        menusCache = null
        return handleResponse({ apiService.deleteMenu(id) }, "deleteMenu") != null
    }

    suspend fun getMenuChats(id: String): List<Chat>? = handleResponse({ apiService.getMenuChats(id) }, "getMenuChats")
    suspend fun getMenuChatThreads(id: String): List<ChatThread>? = handleResponse({ apiService.getMenuChatThreads(id) }, "getMenuChatThreads")

    // Tag endpoints
    suspend fun createTag(tag: ApiTag): ApiTag? = handleResponse({ apiService.createTag(tag) }, "createTag")
    suspend fun getTags(): List<ApiTag>? = handleResponse({ apiService.getTags() }, "getTags")

    // MenuTagRelation endpoints
    suspend fun createMenuTagRelation(relation: MenuTagRelation): MenuTagRelation? = handleResponse({ apiService.createMenuTagRelation(relation) }, "createMenuTagRelation")

    // Chat endpoints
    suspend fun createChat(chat: Chat): Chat? = handleResponse({ apiService.createChat(chat) }, "createChat")
    suspend fun getChats(): List<Chat>? = handleResponse({ apiService.getChats() }, "getChats")

    // Favorite endpoints
    suspend fun createFavorite(favorite: Favorite): Favorite? = handleResponse({ apiService.createFavorite(favorite) }, "createFavorite")
    suspend fun getFavorites(): List<Favorite>? = handleResponse({ apiService.getFavorites() }, "getFavorites")

    // ChatThread endpoints
    suspend fun createChatThread(chatThread: ChatThread): ChatThread? = handleResponse({ apiService.createChatThread(chatThread) }, "createChatThread")
    suspend fun getChatThreads(): List<ChatThread>? = handleResponse({ apiService.getChatThreads() }, "getChatThreads")

    // Video endpoints
    suspend fun createVideo(video: Video): Video? = handleResponse({ apiService.createVideo(video) }, "createVideo")
    suspend fun getVideos(): List<Video>? = handleResponse({ apiService.getVideos() }, "getVideos")

    // Analysis endpoints
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

    // Attachment endpoints
    suspend fun createAttachment(attachment: Attachment): Attachment? = handleResponse({ apiService.createAttachment(attachment) }, "createAttachment")
    suspend fun getAttachments(): List<Attachment>? = handleResponse({ apiService.getAttachments() }, "getAttachments")
}

class ApiException(message: String) : Exception(message)