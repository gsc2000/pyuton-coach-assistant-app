package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Menu // ★ PracticeMenuからMenuに変更
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PracticeListViewModel(private val repository: SwimmingRepository) : ViewModel() {

    // ★ データクラスを正しいMenuに変更
    private val _practiceMenus = MutableStateFlow<List<Menu>>(emptyList())
    val practiceMenus: StateFlow<List<Menu>> = _practiceMenus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPracticeMenus()
    }

    fun loadPracticeMenus() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // ★★★ ここから修正 ★★★
                // 1. 自分のユーザー情報を取得
                val currentUser = repository.getMe()
                if (currentUser != null) {
                    // 2. ユーザーIDを使って練習メニューを取得
                    val menus = repository.getMenusByUserId(currentUser.userId)
                    _practiceMenus.value = menus ?: emptyList()
                } else {
                    _errorMessage.value = "ユーザー情報が取得できませんでした。再度ログインしてください。"
                }
                // ★★★ ここまで修正 ★★★
            } catch (e: Exception) {
                _errorMessage.value = "練習メニューの取得に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}