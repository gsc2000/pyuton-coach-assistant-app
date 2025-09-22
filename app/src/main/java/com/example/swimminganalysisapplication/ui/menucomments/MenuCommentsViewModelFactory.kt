package com.example.swimminganalysisapplication.ui.menucomments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import java.lang.IllegalArgumentException

class MenuCommentsViewModelFactory(
    private val repository: SwimmingRepository,
    private val userPreferences: UserPreferences,
    private val menuId: String // ★ Int から String に変更
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuCommentsViewModel::class.java)) {
            return MenuCommentsViewModel(repository, userPreferences, menuId) as T // menuId をそのまま渡す
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}