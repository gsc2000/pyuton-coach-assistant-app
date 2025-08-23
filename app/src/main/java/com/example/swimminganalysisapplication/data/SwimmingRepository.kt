package com.example.swimminganalysisapplication.data

import android.util.Log
import com.example.swimminganalysisapplication.data.remote.ApiService
import com.example.swimminganalysisapplication.data.remote.model.Race
import com.example.swimminganalysisapplication.data.remote.model.Result
import com.example.swimminganalysisapplication.data.remote.model.Swimmer

// ApiServiceをコンストラクタで受け取ることで、テスト時にモックを注入しやすくなります。
class SwimmingRepository(private val apiService: ApiService) {

    private fun <T> handleResponse(response: retrofit2.Response<T>, successMessage: String, entityName: String, operation: String, entityId: String? = null): T? {
        if (response.isSuccessful) {
            Log.i("SwimmingRepository", "$successMessage - $entityName ${entityId ?: ""} $operation successful.")
            return response.body()
        } else {
            val errorMsg = "Failed to $operation $entityName ${entityId ?: ""}: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}"
            Log.e("SwimmingRepository", errorMsg)
            throw ApiException("$operation $entityName に失敗しました: ${response.code()}")
        }
    }

    private fun <T> handleListResponse(response: retrofit2.Response<List<T>>, entityNamePlural: String): List<T> {
        if (response.isSuccessful) {
            Log.i("SwimmingRepository", "Successfully fetched all $entityNamePlural.")
            return response.body() ?: emptyList()
        } else {
            val errorMsg = "Failed to get all $entityNamePlural: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}"
            Log.e("SwimmingRepository", errorMsg)
            throw ApiException("$entityNamePlural の取得に失敗しました: ${response.code()}")
        }
    }

    private fun handleDeleteResponse(response: retrofit2.Response<Unit>, entityName: String, entityId: Int): Boolean {
        if (response.isSuccessful) {
            Log.i("SwimmingRepository", "$entityName $entityId deleted successfully.")
            return true
        } else {
            val errorMsg = "Failed to delete $entityName $entityId: ${response.code()} ${response.message()} - ${response.errorBody()?.string()}"
            Log.e("SwimmingRepository", errorMsg)
            throw ApiException("$entityName の削除に失敗しました: ${response.code()}")
        }
    }


    // --- Swimmer ---
    suspend fun getAllSwimmers(): List<Swimmer> {
        return handleListResponse(apiService.getAllSwimmers(), "swimmers")
    }

    suspend fun getSwimmer(id: Int): Swimmer? {
        return handleResponse(apiService.getSwimmer(id), "Fetched swimmer", "Swimmer", "get", id.toString())
    }

    suspend fun createSwimmer(swimmer: Swimmer): Swimmer? {
        return handleResponse(apiService.createSwimmer(swimmer), "Created swimmer", "Swimmer", "create")
    }

    suspend fun updateSwimmer(id: Int, swimmer: Swimmer): Swimmer? {
        return handleResponse(apiService.updateSwimmer(id, swimmer), "Updated swimmer", "Swimmer", "update", id.toString())
    }

    suspend fun deleteSwimmer(id: Int): Boolean {
        return handleDeleteResponse(apiService.deleteSwimmer(id), "Swimmer", id)
    }

    // --- Race ---
    suspend fun getAllRaces(): List<Race> {
        return handleListResponse(apiService.getAllRaces(), "races")
    }

    suspend fun getRace(id: Int): Race? {
        return handleResponse(apiService.getRace(id), "Fetched race", "Race", "get", id.toString())
    }

    suspend fun createRace(race: Race): Race? {
        return handleResponse(apiService.createRace(race), "Created race", "Race", "create")
    }

    suspend fun updateRace(id: Int, race: Race): Race? {
        return handleResponse(apiService.updateRace(id, race), "Updated race", "Race", "update", id.toString())
    }

    suspend fun deleteRace(id: Int): Boolean {
        return handleDeleteResponse(apiService.deleteRace(id), "Race", id)
    }

    // --- Result ---
    suspend fun getAllResults(): List<Result> {
        return handleListResponse(apiService.getAllResults(), "results")
    }

    suspend fun getResult(id: Int): Result? {
        return handleResponse(apiService.getResult(id), "Fetched result", "Result", "get", id.toString())
    }

    suspend fun createResult(result: Result): Result? {
        return handleResponse(apiService.createResult(result), "Created result", "Result", "create")
    }

    suspend fun updateResult(id: Int, result: Result): Result? {
        return handleResponse(apiService.updateResult(id, result), "Updated result", "Result", "update", id.toString())
    }

    suspend fun deleteResult(id: Int): Boolean {
        return handleDeleteResponse(apiService.deleteResult(id), "Result", id)
    }
}

// カスタム例外クラス（ファイルの下部や別ファイルに定義）
class ApiException(message: String) : Exception(message)
