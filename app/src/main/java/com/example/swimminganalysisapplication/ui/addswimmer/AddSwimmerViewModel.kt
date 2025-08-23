package com.example.swimminganalysisapplication.ui.addswimmer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.SwimmerRequest
import kotlinx.coroutines.launch

// 登録処理の状態を表す sealed interface
sealed interface AddSwimmerUiState {
    object Idle : AddSwimmerUiState // 初期状態、または登録処理が完了した後
    object Loading : AddSwimmerUiState // 登録処理中
    data class Success(val swimmerName: String) : AddSwimmerUiState // 登録成功
    data class Error(val message: String) : AddSwimmerUiState // 登録失敗
}

class AddSwimmerViewModel(private val repository: SwimmingRepository) : ViewModel() {

    var swimmerName by mutableStateOf("")
    var swimmerAge by mutableStateOf("")
    var swimmerTeam by mutableStateOf("")

    var uiState: AddSwimmerUiState by mutableStateOf(AddSwimmerUiState.Idle)
        private set

    fun updateName(name: String) {
        swimmerName = name
    }

    fun updateAge(age: String) {
        swimmerAge = age
    }

    fun updateTeam(team: String) {
        swimmerTeam = team
    }

    fun addSwimmer() {
        if (swimmerName.isBlank()) {
            uiState = AddSwimmerUiState.Error("選手名は必須です。")
            return
        }

        val ageInt = swimmerAge.toIntOrNull()
        if (swimmerAge.isNotBlank() && ageInt == null) {
            uiState = AddSwimmerUiState.Error("年齢は数値で入力してください。")
            return
        }

        viewModelScope.launch {
            uiState = AddSwimmerUiState.Loading
            try {
                val request = SwimmerRequest(
                    name = swimmerName,
                    age = ageInt,
                    team = swimmerTeam.ifBlank { null } // 空の場合はnullとして送信
                )
                val createdSwimmer = repository.createSwimmer(request)
                if (createdSwimmer != null) { // ★ null チェックを追加
                    uiState = AddSwimmerUiState.Success(createdSwimmer.name)
                } else {
                    // 予期せずnullが返ってきた場合のエラー処理
                    uiState = AddSwimmerUiState.Error("選手登録に成功しましたが、レスポンスが不正です。")
                }
            } catch (e: Exception) {
                uiState = AddSwimmerUiState.Error("選手登録に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    // 登録成功/失敗メッセージ表示後に状態をリセットする関数
    fun consumedUiState() {
        uiState = AddSwimmerUiState.Idle
    }
}

// ViewModel Factory
class AddSwimmerViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddSwimmerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddSwimmerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
