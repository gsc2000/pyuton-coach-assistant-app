package com.example.swimminganalysisapplication.ui.analysis

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisDetailUiState(
    val videoUri: Uri? = null,
    val analysisDetail: AnalysisDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnalysisViewModel(
    private val repository: SwimmingRepository
) : ViewModel() {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _analysisDetailState = MutableStateFlow(AnalysisDetailUiState())
    val analysisDetailState: StateFlow<AnalysisDetailUiState> = _analysisDetailState.asStateFlow()


    init {
        loadVideos()
    }

    fun loadAnalysisDetails(context: Context, videoId: Int, videoUuid: String) {
        viewModelScope.launch {
            _analysisDetailState.value = AnalysisDetailUiState(isLoading = true)
            try {
                // Mock JSON data (as API for this is not ready)
                val mockJsonString = """
                {
                    "analysis_id": ${videoId},
                    "video_name": "result_video_${videoId}.mp4",
                    "analysis_date": "2025-10-27",
                    "total_time": 130.2,
                    "total_stroke_count": 68,
                    "lap_times": [
                        { "lap_number": 1, "lap_time": 30.1, "stroke_count": 16 },
                        { "lap_number": 2, "lap_time": 32.5, "stroke_count": 17 },
                        { "lap_number": 3, "lap_time": 33.4, "stroke_count": 18 },
                        { "lap_number": 4, "lap_time": 34.2, "stroke_count": 17 }
                    ]
                }
                """.trimIndent()
                val analysisDetail = Gson().fromJson(mockJsonString, AnalysisDetail::class.java)

                // Fetch video
                val videoFile = repository.getResultVideo(context, videoUuid)

                _analysisDetailState.value = AnalysisDetailUiState(
                    videoUri = videoFile?.let { Uri.fromFile(it) },
                    analysisDetail = analysisDetail,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("AnalysisViewModel", "Failed to load analysis details", e)
                _analysisDetailState.value = AnalysisDetailUiState(
                    isLoading = false,
                    errorMessage = "詳細の読み込みに失敗しました: ${e.message}"
                )
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
