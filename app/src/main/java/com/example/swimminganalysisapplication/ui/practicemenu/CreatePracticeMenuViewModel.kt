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

// ★★★ UiStateの定義を明確化 ★★★
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object LoadSuccess : UiState() // 読み込み成功
    object SaveSuccess : UiState() // 保存成功
    data class Error(val message: String) : UiState()
}

class CreatePracticeMenuViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _practiceMenu = MutableStateFlow<Menu?>(null)
    val practiceMenu: StateFlow<Menu?> = _practiceMenu.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isEditable = MutableStateFlow(false)
    val isEditable: StateFlow<Boolean> = _isEditable.asStateFlow()

    fun loadMenu(menuId: Int?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val currentUser = repository.getMe()
                if (currentUser == null) {
                    _isEditable.value = false
                    throw Exception("ユーザー情報が取得できません。再度ログインしてください。")
                }

                if (menuId == null) {
                    _isEditable.value = true
                    _practiceMenu.value = null
                } else {
                    val menu = repository.getMenu(menuId)
                    _practiceMenu.value = menu
                    _isEditable.value = menu?.userId == currentUser.userId
                }
                // ★★★ 状態をLoadSuccessに変更 ★★★
                _uiState.value = UiState.LoadSuccess
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun saveMenu(title: String, description: String, items: List<PracticeMenuItem>, isPublic: Boolean, tags: List<String>, existingMenuId: Int?) {
        viewModelScope.launch {
            if (!_isEditable.value) {
                _uiState.value = UiState.Error("このメニューを編集する権限がありません。")
                return@launch
            }

            _uiState.value = UiState.Loading
            try {
                val itemsJson = Gson().toJson(items)
                val fullDescription = "$description\n\n---items---\n$itemsJson".trimIndent()

                if (existingMenuId != null) {
                    val menuToUpdate = repository.getMenu(existingMenuId)?.copy(
                        menuTitle = title,
                        menuDescription = fullDescription,
                        menuIsPublic = isPublic
                    )
                    if (menuToUpdate != null) {
                        repository.updateMenu(existingMenuId, menuToUpdate)
                    } else {
                        throw Exception("更新対象のメニューが見つかりません。")
                    }
                } else {
                    val currentUser = repository.getMe()
                    if (currentUser == null) {
                        throw Exception("ユーザー情報が取得できません。再度ログインしてください。")
                    }
                    val newMenu = Menu(
                        menuId = 0,
                        menuOrgId = 0,
                        menuTitle = title,
                        menuDescription = fullDescription,
                        menuIsPublic = isPublic,
                        menuIsForked = false,
                        menuForkedFromMenuId = null,
                        menuVersion = 1,
                        menuCreateAt = "",
                        menuUpdateAt = "",
                        userId = currentUser.userId,
                        playerId = null,
                        menuTagId = null
                    )
                    repository.createMenu(newMenu)
                }
                // ★★★ 状態をSaveSuccessに変更 ★★★
                _uiState.value = UiState.SaveSuccess
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "メニューの保存に失敗しました。")
            }
        }
    }

    // ★★★ 状態をリセットするメソッドを追加 ★★★
    fun resetUiState() {
        _uiState.value = UiState.Idle
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


class CreatePracticeMenuViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePracticeMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePracticeMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}