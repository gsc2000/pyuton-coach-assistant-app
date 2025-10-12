package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatePracticeMenuViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _practiceMenu = MutableStateFlow<Menu?>(null)
    val practiceMenu: StateFlow<Menu?> = _practiceMenu.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadMenu(menuId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val menu = repository.getMenu(menuId)
                Log.d("DEBUG_MENU", "Loaded menu from repository: $menu")
                _practiceMenu.value = menu
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun saveMenu(title: String, description: String, items: List<PracticeMenuItem>, isPublic: Boolean, tags: List<String>, existingMenuId: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // itemsをJSON文字列に変換してdescriptionに含める
                val itemsJson = Gson().toJson(items)
                val fullDescription = """
                    $description
                    
                    ---items---
                    $itemsJson
                """.trimIndent()

                if (existingMenuId != null) {
                    // Update existing menu
                    val menuToUpdate = repository.getMenu(existingMenuId)?.copy(
                        menuTitle = title,
                        menuDescription = fullDescription,
                        menuIsPublic = isPublic
                        // tags are handled separately
                    )
                    if (menuToUpdate != null) {
                        repository.updateMenu(existingMenuId, menuToUpdate)
                    } else {
                        throw Exception("更新対象のメニューが見つかりません。")
                    }
                } else {
                    // Create new menu
                    val currentUser = repository.getMe()
                    if (currentUser == null) {
                        throw Exception("ユーザー情報が取得できません。再度ログインしてください。")
                    }

                    // ★★★ 正しいユーザーIDを設定し、orgIdは0を仮設定 ★★★
                    val newMenu = Menu(
                        menuId = 0, // サーバー側で自動採番
                        menuOrgId = 0, // サーバーのデフォルト値に期待
                        menuTitle = title,
                        menuDescription = fullDescription,
                        menuIsPublic = isPublic,
                        menuIsForked = false,
                        menuForkedFromMenuId = null,
                        menuVersion = 1,
                        menuCreateAt = "", // サーバー側で設定
                        menuUpdateAt = "",  // サーバー側で設定
                        userId = currentUser.userId, // ★★★ 正しいユーザーID ★★★
                        playerId = null,
                        menuTagId = null
                    )
                    repository.createMenu(newMenu)
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "メニューの保存に失敗しました。")
            }
        }
    }

    fun getItemsFromJson(description: String?): Pair<String, List<PracticeMenuItem>> {
        if (description == null) return "" to emptyList()
        val separator = "---items---"
        val parts = description.split(separator)
        val mainDescription = parts.getOrNull(0)?.trim() ?: ""
        val json = parts.getOrNull(1)?.trim()

        return if (json != null) {
            try {
                val type = object : TypeToken<List<PracticeMenuItem>>() {}.type
                val items = Gson().fromJson<List<PracticeMenuItem>>(json, type)
                mainDescription to items
            } catch (e: Exception) {
                mainDescription to emptyList()
            }
        } else {
            description to emptyList()
        }
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

// ViewModelFactoryは変更なし
class CreatePracticeMenuViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePracticeMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePracticeMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}