package com.example.swimminganalysisapplication.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.swimminganalysisapplication.data.ApiException
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Analysis
import com.example.swimminganalysisapplication.data.remote.model.AnalysisDetail
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val repository: SwimmingRepository,
    private val playerId: Int?
) : ViewModel() {

    private val _analysisResults = MutableStateFlow<List<Analysis>>(emptyList())
    val analysisResults: StateFlow<List<Analysis>> = _analysisResults

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _selectedAnalysisDetail = MutableStateFlow<AnalysisDetail?>(null)
    val selectedAnalysisDetail: StateFlow<AnalysisDetail?> = _selectedAnalysisDetail

    private val _showDetailDialog = MutableStateFlow(false)
    val showDetailDialog: StateFlow<Boolean> = _showDetailDialog

    init {
        loadAnalysisResults()
    }

    fun fetchAnalysisJson(analysis: Analysis) {
        viewModelScope.launch {
            // TODO: Replace this mock implementation with actual API call
            val mockJsonString = """
            {
                "analysis_id": ${analysis.analysisId},
                "video_name": "mock_video_${analysis.analysisId}.mp4",
                "analysis_date": "2025-09-14",
                "total_time": 125.5,
                "total_stroke_count": 64,
                "lap_times": [
                    { "lap_number": 1, "lap_time": 29.8, "stroke_count": 15 },
                    { "lap_number": 2, "lap_time": 31.2, "stroke_count": 16 },
                    { "lap_number": 3, "lap_time": 32.0, "stroke_count": 17 },
                    { "lap_number": 4, "lap_time": 32.5, "stroke_count": 16 }
                ]
            }
            """.trimIndent()

            try {
                val gson = Gson()
                val detail = gson.fromJson(mockJsonString, AnalysisDetail::class.java)
                _selectedAnalysisDetail.value = detail
                _showDetailDialog.value = true
                Log.i("AnalysisViewModel", "Successfully parsed mock JSON for analysis ${analysis.analysisId}")
            } catch (e: Exception) {
                _errorMessage.value = "分析詳細(JSON)の解析中にエラーが発生しました: ${e.message}"
                Log.e("AnalysisViewModel", "Error parsing mock JSON for analysis ${analysis.analysisId}", e)
            }
        }
    }

    fun dismissDetailDialog() {
        _showDetailDialog.value = false
        _selectedAnalysisDetail.value = null
    }

    private fun loadAnalysisResults() {
        viewModelScope.launch {
            // TODO: Replace this mock implementation with actual API call
            _analysisResults.value = listOf(
                Analysis(analysisId = 1, video1Id = 101, video2Id = 102, userId = 1, videoCompareAnalysisJsonPath = "/path/to/json1.json", menuId = 1),
                Analysis(analysisId = 2, video1Id = 103, video2Id = null, userId = 1, videoCompareAnalysisJsonPath = "/path/to/json2.json", menuId = 1),
                Analysis(analysisId = 3, video1Id = 104, video2Id = 105, userId = 2, videoCompareAnalysisJsonPath = "/path/to/json3.json", menuId = 2)
            )
            /*
            try {
                val results = repository.getAnalyses()
                if (results != null) {
                    // TODO: The Analysis model does not have playerId.
                    // Filtering logic will be implemented in the next step.
                    _analysisResults.value = results
                } else {
                    _errorMessage.value = "分析結果の取得に失敗しました。"
                }
            } catch (e: ApiException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = "予期せぬエラーが発生しました: ${e.message}"
            }
            */
        }
    }
}
