package com.example.swimminganalysisapplication.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository

class AnalysisViewModelFactory(
    private val repository: SwimmingRepository,
    private val playerId: Int?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalysisViewModel(repository, playerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
