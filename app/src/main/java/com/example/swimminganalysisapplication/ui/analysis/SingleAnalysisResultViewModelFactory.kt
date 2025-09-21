package com.example.swimminganalysisapplication.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository

class SingleAnalysisResultViewModelFactory(
    private val repository: SwimmingRepository,
    private val analysisId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleAnalysisResultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SingleAnalysisResultViewModel(repository, analysisId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
