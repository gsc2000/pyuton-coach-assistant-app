package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnalysisDetail(
    @SerializedName("analysis_id")
    val analysisId: Int,
    @SerializedName("video_name")
    val videoName: String,
    @SerializedName("analysis_date")
    val analysisDate: String,
    @SerializedName("total_time")
    val totalTime: Double,
    @SerializedName("total_stroke_count")
    val totalStrokeCount: Int,
    @SerializedName("lap_times")
    val lapTimes: List<LapTime>
)
