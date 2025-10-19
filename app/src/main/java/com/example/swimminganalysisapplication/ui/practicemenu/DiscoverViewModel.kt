package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Menu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _publicMenus = MutableStateFlow<List<Menu>>(emptyList())
    val publicMenus: StateFlow<List<Menu>> = _publicMenus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _forkResult = MutableStateFlow<Result<Unit>?>(null)
    val forkResult: StateFlow<Result<Unit>?> = _forkResult.asStateFlow()

    init {
        loadPublicMenus()
    }

    private fun loadPublicMenus() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // ★★★ ここから修正 ★★★
                // 1. 自分のユーザー情報を取得
                val currentUser = repository.getMe()

                // 2. 全ての公開メニューを取得
                val allPublicMenus = repository.getPublicMenus() ?: emptyList()

                if (currentUser != null) {
                    // 3. 自分のメニューを除外してUIに反映
                    _publicMenus.value = allPublicMenus.filter { it.userId != currentUser.userId }
                } else {
                    // ログインしていない、またはユーザー情報が取れない場合は全て表示
                    _publicMenus.value = allPublicMenus
                    // エラーメッセージを表示しても良い
                    // _errorMessage.value = "ユーザー情報が取得できず、自分のメニューを非表示にできませんでした。"
                }
                // ★★★ ここまで修正 ★★★
            } catch (e: Exception) {
                _errorMessage.value = "公開メニューの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forkMenu(menuId: Int) {
        viewModelScope.launch {
            try {
                repository.forkMenu(menuId)
                _forkResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _forkResult.value = Result.failure(e)
            }
        }
    }

    fun consumeForkResult() {
        _forkResult.value = null
    }
}