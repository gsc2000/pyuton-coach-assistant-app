package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.Comment
import com.example.swimminganalysisapplication.data.remote.model.Favorite
import com.example.swimminganalysisapplication.data.remote.model.PracticeMenu
import com.example.swimminganalysisapplication.data.remote.model.User
import java.util.UUID

class SwimmingRepository(private val apiService: ApiService) {

    private suspend fun <T> handleResponse(
        apiCall: suspend () -> retrofit2.Response<T>,
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


    // --- Auth ---
    suspend fun register(user: Map<String, String>): User? {
        return handleResponse({ apiService.register(user) }, "User registered", "register")
    }

    suspend fun login(credentials: Map<String, String>): Map<String, String>? {
        return handleResponse({ apiService.login(credentials) }, "User logged in", "login")
    }

    suspend fun getMe(): User? {
        return handleResponse({ apiService.getMe() }, "Fetched user profile", "getMe")
    }

    // --- PracticeMenu ---
    suspend fun createMenu(practiceMenu: PracticeMenu): PracticeMenu? {
        return handleResponse({ apiService.createMenu(practiceMenu) }, "Created menu", "createMenu")
    }

    suspend fun getMyMenus(): List<PracticeMenu>? {
        return handleResponse({ apiService.getMyMenus() }, "Fetched my menus", "getMyMenus")
    }

    suspend fun getPublicMenus(query: String?, tags: String?, page: Int?, limit: Int?): List<PracticeMenu>? {
        return handleResponse({ apiService.getPublicMenus(query, tags, page, limit) }, "Fetched public menus", "getPublicMenus")
    }

    suspend fun getMenuById(menuId: UUID): PracticeMenu? {
        return handleResponse({ apiService.getMenuById(menuId) }, "Fetched menu by ID", "getMenuById")
    }

    suspend fun updateMenu(menuId: UUID, practiceMenu: PracticeMenu): PracticeMenu? {
        return handleResponse({ apiService.updateMenu(menuId, practiceMenu) }, "Updated menu", "updateMenu")
    }

    suspend fun deleteMenu(menuId: UUID): Boolean {
        return handleResponse({ apiService.deleteMenu(menuId) }, "Deleted menu", "deleteMenu") != null
    }

    suspend fun forkMenu(publicMenuId: UUID): PracticeMenu? {
        return handleResponse({ apiService.forkMenu(publicMenuId) }, "Forked menu", "forkMenu")
    }

    suspend fun checkUpdate(menuId: UUID): Map<String, Boolean>? {
        return handleResponse({ apiService.checkUpdate(menuId) }, "Checked for menu update", "checkUpdate")
    }

    suspend fun pullUpdate(menuId: UUID): PracticeMenu? {
        return handleResponse({ apiService.pullUpdate(menuId) }, "Pulled menu update", "pullUpdate")
    }

    // --- Comment ---
    suspend fun postComment(menuId: UUID, comment: Map<String, String>): Comment? {
        return handleResponse({ apiService.postComment(menuId, comment) }, "Posted comment", "postComment")
    }

    suspend fun getComments(menuId: UUID): List<Comment>? {
        return handleResponse({ apiService.getComments(menuId) }, "Fetched comments", "getComments")
    }

    suspend fun deleteComment(commentId: UUID): Boolean {
        return handleResponse({ apiService.deleteComment(commentId) }, "Deleted comment", "deleteComment") != null
    }

    // --- Favorite ---
    suspend fun addFavorite(publicMenuId: UUID): Favorite? {
        return handleResponse({ apiService.addFavorite(publicMenuId) }, "Added favorite", "addFavorite")
    }

    suspend fun removeFavorite(publicMenuId: UUID): Boolean {
        return handleResponse({ apiService.removeFavorite(publicMenuId) }, "Removed favorite", "removeFavorite") != null
    }

    suspend fun getFavorites(): List<Favorite>? {
        return handleResponse({ apiService.getFavorites() }, "Fetched favorites", "getFavorites")
    }
}

class ApiException(message: String) : Exception(message)
