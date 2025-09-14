package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class AnalysisResult(
    @SerializedName("analysis_id")
    val analysisId: Int,
    @SerializedName("player_id")
    val playerId: Int,
    @SerializedName("video_name")
    val videoName: String,
    @SerializedName("analysis_date")
    val analysisDate: String,
    @SerializedName("swim_time")
    val swimTime: Double,
    @SerializedName("stroke_count")
    val strokeCount: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
