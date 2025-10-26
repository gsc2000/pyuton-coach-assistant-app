package com.example.swimminganalysisapplication.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.swimminganalysisapplication.data.ApiException
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.AnalysisDetail
import com.example.swimminganalysisapplication.data.remote.model.Video
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val repository: SwimmingRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _selectedAnalysisDetail = MutableStateFlow<AnalysisDetail?>(null)
    val selectedAnalysisDetail: StateFlow<AnalysisDetail?> = _selectedAnalysisDetail

    init {
        loadVideos()
    }

    fun fetchAnalysisJson(videoId: Int) {
        viewModelScope.launch {
            // TODO: Replace this mock implementation with actual API call
            val mockJsonString = """
            {
                "analysis_id": ${videoId},
                "video_name": "mock_video_${videoId}.mp4",
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
                Log.i("AnalysisViewModel", "Successfully parsed mock JSON for video ${videoId}")
            } catch (e: Exception) {
                _errorMessage.value = "分析詳細(JSON)の解析中にエラーが発生しました: ${e.message}"
                Log.e("AnalysisViewModel", "Error parsing mock JSON for video ${videoId}", e)
            }
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            try {
                val results = repository.getVideos()
                if (results != null) {
                    _videos.value = results
                } else {
                    _errorMessage.value = "分析結果の取得に失敗しました。"
                }
            } catch (e: ApiException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = "予期せぬエラーが発生しました: ${e.message}"
            }
        }
    }
}
