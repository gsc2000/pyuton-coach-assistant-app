package com.example.swimminganalysisapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class Analysis(
    @SerializedName("analysisId")
    val analysisId: Int,
    @SerializedName("video1Id")
    val video1Id: Int,
    @SerializedName("video2Id")
    val video2Id: Int?,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("videoCompareAnalysisJsonPath")
    val videoCompareAnalysisJsonPath: String?,
    @SerializedName("menuId")
    val menuId: Int?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("playerId")
    val playerId: Int?
)
