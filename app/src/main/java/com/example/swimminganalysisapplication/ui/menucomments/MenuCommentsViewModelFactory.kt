package com.example.swimminganalysisapplication.ui.menucomments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository
import java.lang.IllegalArgumentException

class MenuCommentsViewModelFactory(
    private val repository: SwimmingRepository,
    private val menuId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuCommentsViewModel::class.java)) {
            // userPreferencesを渡さないように修正
            return MenuCommentsViewModel(repository, menuId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
