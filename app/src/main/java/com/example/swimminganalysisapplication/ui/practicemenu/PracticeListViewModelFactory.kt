package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.swimminganalysisapplication.data.SwimmingRepository

class PracticeListViewModelFactory(private val repository: SwimmingRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeListViewModel::class.java)) {
            return PracticeListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}