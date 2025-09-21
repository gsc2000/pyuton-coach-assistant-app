package com.example.swimminganalysisapplication.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository

class SingleAnalysisSetupViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SingleAnalysisSetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SingleAnalysisSetupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
