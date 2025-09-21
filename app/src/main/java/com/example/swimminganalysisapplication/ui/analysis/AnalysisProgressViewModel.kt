package com.example.swimminganalysisapplication.ui.analysis

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AnalysisProgressViewModel(
    private val repository: SwimmingRepository,
    private val analysisId: Int
) : ViewModel() {

    private val _analysisStatus = mutableStateOf("解析を開始しています...")
    val analysisStatus: State<String> = _analysisStatus

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val analysis = repository.getAnalysis(analysisId)
                    when (analysis?.status) {
                        "SUCCESS" -> {
                            _analysisStatus.value = "解析が完了しました！"
                            _navigationEvent.emit(NavigationEvent.NavigateToResult(analysisId))
                            break // Stop polling
                        }
                        "FAILURE" -> {
                            _analysisStatus.value = "解析に失敗しました。"
                            _navigationEvent.emit(NavigationEvent.NavigateBack)
                            break // Stop polling
                        }
                        "PROCESSING" -> {
                            _analysisStatus.value = "解析中です..."
                        }
                        "PENDING" -> {
                            _analysisStatus.value = "解析待ちです..."
                        }
                        else -> {
                            _analysisStatus.value = "ステータスを確認中..."
                        }
                    }
                } catch (e: Exception) {
                    _analysisStatus.value = "エラーが発生しました: ${e.message}"
                    _navigationEvent.emit(NavigationEvent.NavigateBack)
                    break // Stop polling on error
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    sealed class NavigationEvent {
        data class NavigateToResult(val analysisId: Int) : NavigationEvent()
        object NavigateBack : NavigationEvent()
    }
}