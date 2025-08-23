package com.example.swimminganalysisapplication.ui.swimmers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Swimmer
import com.example.swimminganalysisapplication.data.ApiException // SwimmingRepositoryと同じ場所にある想定
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log // Log import

// UIの状態を表すデータクラス
data class SwimmersUiState(
    val swimmers: List<Swimmer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SwimmersViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SwimmersUiState())
    val uiState: StateFlow<SwimmersUiState> = _uiState.asStateFlow()

    init {
        loadSwimmers() // ViewModel初期化時に選手一覧を読み込む
    }

    fun loadSwimmers() {
        Log.d("SwimmersViewModel", "loadSwimmers called")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val swimmersList = repository.getAllSwimmers()
                Log.d("SwimmersViewModel", "Swimmers loaded: ${swimmersList.size} items")
                _uiState.value = _uiState.value.copy(
                    swimmers = swimmersList,
                    isLoading = false
                )
            } catch (e: ApiException) {
                Log.e("SwimmersViewModel", "Error loading swimmers: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "選手データの読み込みに失敗しました。"
                )
            } catch (e: Exception) { // その他の予期せぬ例外
                Log.e("SwimmersViewModel", "Unexpected error loading swimmers", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "予期せぬエラーが発生しました。"
                )
            }
        }
    }

    fun createSwimmer(name: String, age: Int?, team: String?) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "選手名は必須です。")
            return
        }
        val newSwimmer = Swimmer(id = 0, name = name, age = age, team = team)
        Log.d("SwimmersViewModel", "createSwimmer called with: $newSwimmer")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val createdSwimmer = repository.createSwimmer(newSwimmer)
                if (createdSwimmer != null) {
                    Log.i("SwimmersViewModel", "Swimmer created successfully: $createdSwimmer")
                    loadSwimmers() 
                } else {
                    Log.w("SwimmersViewModel", "Swimmer creation returned successful but no body")
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "選手の作成に成功しましたが、データがありません。")
                }
            } catch (e: ApiException) {
                Log.e("SwimmersViewModel", "Error creating swimmer: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "選手の作成に失敗しました。"
                )
            } catch (e: Exception) {
                Log.e("SwimmersViewModel", "Unexpected error creating swimmer", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "予期せぬエラーが発生しました。"
                )
            }
        }
    }
}
