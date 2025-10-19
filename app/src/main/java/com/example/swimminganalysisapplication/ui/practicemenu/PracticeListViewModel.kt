package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeListViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _practiceMenus = MutableStateFlow<List<Menu>>(emptyList())
    val practiceMenus: StateFlow<List<Menu>> = _practiceMenus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ★★★ ここを削除 ★★★
    // init {
    //     loadPracticeMenus()
    // }

    fun loadPracticeMenus() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val currentUser = repository.getMe()
                if (currentUser != null) {
                    // forceRefreshをtrueにすることで、常に最新のデータを取得する
                    val menus = repository.getMenusByUserId(currentUser.userId, forceRefresh = true)
                    _practiceMenus.value = menus ?: emptyList()
                } else {
                    _errorMessage.value = "ユーザー情報が取得できませんでした。再度ログインしてください。"
                }
            } catch (e: Exception) {
                _errorMessage.value = "練習メニューの取得に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}