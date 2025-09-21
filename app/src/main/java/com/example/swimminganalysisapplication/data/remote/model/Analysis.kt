package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Analysis(
    @SerializedName("analysis_id")
    val analysisId: Int,
    @SerializedName("video1_id")
    val video1Id: Int,
    @SerializedName("video2_id")
    val video2Id: Int?,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("video_compare_analysis_json_path")
    val videoCompareAnalysisJsonPath: String?,
    @SerializedName("menu_id")
    val menuId: Int?,
    @SerializedName("status")
    val status: String?
)
