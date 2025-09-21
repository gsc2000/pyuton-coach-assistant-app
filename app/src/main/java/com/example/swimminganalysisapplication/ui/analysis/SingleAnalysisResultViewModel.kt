package com.example.swimminganalysisapplication.ui.analysis

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Analysis
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SingleAnalysisResultViewModel(
    private val repository: SwimmingRepository,
    private val analysisId: Int
) : ViewModel() {

    private val _analysis = mutableStateOf<Analysis?>(null)
    val analysis: State<Analysis?> = _analysis

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadAnalysisDetails()
    }

    private fun loadAnalysisDetails() {
        viewModelScope.launch {
            try {
                // In a real scenario, you would fetch detailed analysis data.
                // For now, we fetch the basic analysis object.
                val result = repository.getAnalysis(analysisId)
                _analysis.value = result
            } catch (e: Exception) {
                _errorMessage.value = "解析結果の読み込みに失敗しました: ${e.message}"
            }
        }
    }

    fun onNavigateToAnalysisListClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToAnalysisList)
        }
    }

    sealed class NavigationEvent {
        object NavigateToAnalysisList : NavigationEvent()
    }
}
