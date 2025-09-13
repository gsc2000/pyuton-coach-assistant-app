package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.PracticeMenu
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePracticeMenuViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _practiceMenu = MutableStateFlow<PracticeMenu?>(null)
    val practiceMenu: StateFlow<PracticeMenu?> = _practiceMenu.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadMenu(menuId: UUID) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val menu = repository.getMenuById(menuId)
                _practiceMenu.value = menu
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun saveMenu(title: String, description: String, items: List<PracticeMenuItem>, isPublic: Boolean, tags: List<String>, existingMenuId: UUID?) {
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
                    val menuToUpdate = repository.getMenuById(existingMenuId)?.copy(
                        title = title,
                        description = fullDescription,
                        is_public = isPublic,
                        tags = tags
                    )
                    if (menuToUpdate != null) {
                        repository.updateMenu(existingMenuId, menuToUpdate)
                    } else {
                        throw Exception("Menu not found for updating")
                    }
                } else {
                    // Create new menu
                    val newMenu = PracticeMenu(
                        id = UUID.randomUUID(),
                        user_id = UUID.randomUUID(), // This should come from the logged-in user
                        title = title,
                        description = fullDescription,
                        is_public = isPublic,
                        tags = tags,
                        version = 1,
                        is_forked = false,
                        forked_from_menu_id = null,
                        created_at = "", // Server will set this
                        updated_at = ""  // Server will set this
                    )
                    repository.createMenu(newMenu)
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save menu")
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

class CreatePracticeMenuViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePracticeMenuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePracticeMenuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
